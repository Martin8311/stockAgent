package com.harnessagent.user;

import com.harnessagent.audit.AuditEventService;
import com.harnessagent.audit.RiskLevel;
import com.harnessagent.web.ApiRequestException;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final AuditEventService auditEventService;

    public UserProfileService(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            AuditEventService auditEventService
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.auditEventService = auditEventService;
    }

    @Transactional
    public UserProfileResponse getProfile(Long userId) {
        AppUser user = loadUser(userId);
        UserProfile profile = loadProfile(user);
        return UserProfileResponse.from(user, profile);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateUserProfileRequest request) {
        AppUser user = loadUser(userId);
        UserProfile profile = loadProfile(user);
        profile.update(request.riskPreference(), request.investmentHorizon(), request.capitalPurpose());
        UserProfile savedProfile = userProfileRepository.save(profile);
        auditEventService.record(
                user.getEmail(),
                "USER_PROFILE_UPDATED",
                "User updated investment profile context.",
                RiskLevel.MEDIUM
        );
        return UserProfileResponse.from(user, savedProfile);
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(UserSummaryResponse::from)
                .toList();
    }

    private AppUser loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiRequestException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private UserProfile loadProfile(AppUser user) {
        return userProfileRepository.findById(user.getId())
                .orElseGet(() -> userProfileRepository.save(UserProfile.createDefault(user)));
    }
}
