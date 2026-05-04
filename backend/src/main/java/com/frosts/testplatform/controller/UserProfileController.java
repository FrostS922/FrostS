package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.common.Auditable;
import com.frosts.testplatform.dto.ProfileResponse;
import com.frosts.testplatform.dto.SecurityInfoResponse;
import com.frosts.testplatform.dto.UpdateProfileRequest;
import com.frosts.testplatform.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "用户资料", description = "个人资料与安全信息管理")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/profile")
    @Operation(summary = "获取个人资料")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(Authentication authentication) {
        ProfileResponse profile = userProfileService.getProfile(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/profile")
    @Auditable(action = "UPDATE_PROFILE", target = "USER", description = "更新个人资料")
    @Operation(summary = "更新个人资料")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        ProfileResponse profile = userProfileService.updateProfile(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @GetMapping("/security-info")
    @Operation(summary = "获取安全信息")
    public ResponseEntity<ApiResponse<SecurityInfoResponse>> getSecurityInfo(Authentication authentication) {
        SecurityInfoResponse response = userProfileService.getSecurityInfo(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
