package com.harnessagent.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harnessagent.user.AppRole;
import com.harnessagent.user.AppUser;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final SecurityProperties securityProperties;
    private final Clock clock;

    @Autowired
    public JwtTokenService(ObjectMapper objectMapper, SecurityProperties securityProperties) {
        this(objectMapper, securityProperties, Clock.systemUTC());
    }

    JwtTokenService(ObjectMapper objectMapper, SecurityProperties securityProperties, Clock clock) {
        this.objectMapper = objectMapper;
        this.securityProperties = securityProperties;
        this.clock = clock;
    }

    public String createToken(AppUser user) {
        Instant issuedAt = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plus(securityProperties.tokenTtlMinutes(), ChronoUnit.MINUTES);
        Map<String, Object> header = Map.of(
                "alg", "HS256",
                "typ", "JWT"
        );
        Map<String, Object> payload = Map.of(
                "sub", user.getEmail(),
                "uid", user.getId(),
                "roles", user.getRoles().stream().map(AppRole::name).sorted().toList(),
                "iat", issuedAt.getEpochSecond(),
                "exp", expiresAt.getEpochSecond()
        );

        String unsignedToken = base64UrlJson(header) + "." + base64UrlJson(payload);
        return unsignedToken + "." + sign(unsignedToken);
    }

    public JwtClaims parseAndValidate(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new JwtAuthenticationException("Malformed token");
        }

        String unsignedToken = parts[0] + "." + parts[1];
        String expectedSignature = sign(unsignedToken);
        if (!constantTimeEquals(expectedSignature, parts[2])) {
            throw new JwtAuthenticationException("Invalid token signature");
        }

        Map<String, Object> payload = parseJson(parts[1]);
        String subject = requireString(payload, "sub");
        Long userId = requireLong(payload, "uid");
        Instant issuedAt = Instant.ofEpochSecond(requireLong(payload, "iat"));
        Instant expiresAt = Instant.ofEpochSecond(requireLong(payload, "exp"));
        if (!Instant.now(clock).isBefore(expiresAt)) {
            throw new JwtAuthenticationException("Token expired");
        }

        @SuppressWarnings("unchecked")
        List<String> roleList = (List<String>) payload.getOrDefault("roles", List.of());
        Set<String> roles = roleList.stream().collect(Collectors.toUnmodifiableSet());
        return new JwtClaims(userId, subject, roles, issuedAt, expiresAt);
    }

    private String base64UrlJson(Map<String, Object> data) {
        try {
            return base64Url(objectMapper.writeValueAsBytes(data));
        } catch (Exception ex) {
            throw new JwtAuthenticationException("Unable to encode token", ex);
        }
    }

    private Map<String, Object> parseJson(String encoded) {
        try {
            byte[] json = Base64.getUrlDecoder().decode(encoded);
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new JwtAuthenticationException("Unable to decode token", ex);
        }
    }

    private String sign(String unsignedToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            byte[] secret = securityProperties.jwtSecret().getBytes(StandardCharsets.UTF_8);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return base64Url(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new JwtAuthenticationException("Unable to sign token", ex);
        }
    }

    private String base64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private boolean constantTimeEquals(String left, String right) {
        return MessageDigestUtil.constantTimeEquals(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String requireString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        throw new JwtAuthenticationException("Missing token claim: " + key);
    }

    private Long requireLong(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new JwtAuthenticationException("Missing token claim: " + key);
    }
}
