package com.harnessagent.user;

import com.harnessagent.security.AuthenticatedUser;
import com.harnessagent.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class CurrentUserController {

    private final UserProfileService userProfileService;

    public CurrentUserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    @Operation(summary = "Get current authenticated user profile")
    public ApiResponse<UserProfileResponse> me(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(userProfileService.getProfile(user.id()));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update current user investment profile context")
    public ApiResponse<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody UpdateUserProfileRequest request
    ) {
        return ApiResponse.ok(userProfileService.updateProfile(user.id(), request), "Profile updated");
    }
}

