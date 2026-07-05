package com.harnessagent.auth;

import com.harnessagent.audit.AuditEventService;
import com.harnessagent.audit.RiskLevel;
import com.harnessagent.security.JwtClaims;
import com.harnessagent.security.JwtTokenService;
import com.harnessagent.user.AppRole;
import com.harnessagent.user.AppUser;
import com.harnessagent.user.UserProfile;
import com.harnessagent.user.UserProfileRepository;
import com.harnessagent.user.UserRepository;
import com.harnessagent.web.ApiRequestException;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final AuditEventService auditEventService;

    public AuthService(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            AuditEventService auditEventService
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.auditEventService = auditEventService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiRequestException(HttpStatus.CONFLICT, "Email is already registered");
        }

        Set<AppRole> roles = userRepository.count() == 0
                ? EnumSet.of(AppRole.USER, AppRole.ADMIN)
                : EnumSet.of(AppRole.USER);
        AppUser user = AppUser.create(
                email,
                passwordEncoder.encode(request.password()),
                request.displayName().trim(),
                roles
        );
        AppUser savedUser = userRepository.save(user);
        userProfileRepository.save(UserProfile.createDefault(savedUser));

        auditEventService.record(
                savedUser.getEmail(),
                "USER_REGISTERED",
                "User registered with roles " + savedUser.getRoles(),
                RiskLevel.MEDIUM
        );
        return createAuthResponse(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        auditEventService.record(
                user.getEmail(),
                "USER_LOGIN_SUCCEEDED",
                "User logged in successfully.",
                RiskLevel.LOW
        );
        return createAuthResponse(user);
    }

    private AuthResponse createAuthResponse(AppUser user) {
        String token = jwtTokenService.createToken(user);
        JwtClaims claims = jwtTokenService.parseAndValidate(token);
        return new AuthResponse(
                "Bearer",
                token,
                claims.expiresAt(),
                new AuthResponse.UserAccountResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getDisplayName(),
                        user.getRoles()
                )
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
