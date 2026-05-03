package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.common.Auditable;
import com.frosts.testplatform.dto.ProfileResponse;
import com.frosts.testplatform.dto.SecurityInfoResponse;
import com.frosts.testplatform.dto.UpdateProfileRequest;
import com.frosts.testplatform.entity.LoginHistory;
import com.frosts.testplatform.entity.User;
import com.frosts.testplatform.repository.LoginHistoryRepository;
import com.frosts.testplatform.repository.UserRepository;
import com.frosts.testplatform.service.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.stream.Collectors;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final FileStorageService fileStorageService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("用户不存在: " + authentication.getName()));

        ProfileResponse profile = toProfileResponse(user);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/profile")
    @Auditable(action = "UPDATE_PROFILE", target = "USER", description = "更新个人资料")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("用户不存在: " + authentication.getName()));

        if (request.realName() != null) {
            user.setRealName(request.realName().isBlank() ? null : request.realName().trim());
        }
        if (request.email() != null) {
            String email = request.email().isBlank() ? null : request.email().trim();
            if (email != null && !email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
                throw new RuntimeException("邮箱已被使用: " + email);
            }
            user.setEmail(email);
        }
        if (request.phone() != null) {
            user.setPhone(request.phone().isBlank() ? null : request.phone().trim());
        }
        if (request.avatar() != null) {
            String oldAvatar = user.getAvatar();
            String newAvatar = request.avatar().isBlank() ? null : request.avatar().trim();
            if (oldAvatar != null && !oldAvatar.equals(newAvatar)) {
                fileStorageService.deleteAvatar(oldAvatar);
            }
            user.setAvatar(newAvatar);
        }
        if (request.department() != null) {
            user.setDepartment(request.department().isBlank() ? null : request.department().trim());
        }
        if (request.position() != null) {
            user.setPosition(request.position().isBlank() ? null : request.position().trim());
        }

        userRepository.save(user);

        ProfileResponse profile = toProfileResponse(user);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @GetMapping("/security-info")
    public ResponseEntity<ApiResponse<SecurityInfoResponse>> getSecurityInfo(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("用户不存在: " + authentication.getName()));

        Page<LoginHistory> recentLogins = loginHistoryRepository.findByUsernameOrderByLoginAtDesc(
                authentication.getName(), PageRequest.of(0, 20));

        SecurityInfoResponse response = SecurityInfoResponse.builder()
                .passwordChangedAt(user.getPasswordChangedAt())
                .accountNonLocked(user.getAccountNonLocked())
                .lockReason(user.getLockReason())
                .loginFailCount(user.getLoginFailCount())
                .lastLoginAt(user.getLastLoginAt())
                .lastLoginIp(user.getLastLoginIp())
                .loginCount(user.getLoginCount())
                .recentLogins(recentLogins.getContent().stream()
                        .map(this::toLoginHistoryItem)
                        .collect(Collectors.toList()))
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private SecurityInfoResponse.LoginHistoryItem toLoginHistoryItem(LoginHistory history) {
        return SecurityInfoResponse.LoginHistoryItem.builder()
                .id(history.getId())
                .loginAt(history.getLoginAt())
                .loginIp(history.getLoginIp())
                .userAgent(history.getUserAgent())
                .success(history.getSuccess())
                .failReason(history.getFailReason())
                .build();
    }

    private ProfileResponse toProfileResponse(User user) {
        return ProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .department(user.getDepartment())
                .position(user.getPosition())
                .enabled(user.getEnabled())
                .accountNonLocked(user.getAccountNonLocked())
                .lastLoginAt(user.getLastLoginAt())
                .lastLoginIp(user.getLastLoginIp())
                .loginCount(user.getLoginCount())
                .passwordChangedAt(user.getPasswordChangedAt())
                .roles(user.getRoles().stream()
                        .map(role -> "ROLE_" + role.getCode())
                        .collect(Collectors.toList()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
