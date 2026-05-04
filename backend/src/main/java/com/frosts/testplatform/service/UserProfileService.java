package com.frosts.testplatform.service;

import com.frosts.testplatform.dto.ProfileResponse;
import com.frosts.testplatform.dto.SecurityInfoResponse;
import com.frosts.testplatform.dto.UpdateProfileRequest;
import com.frosts.testplatform.entity.LoginHistory;
import com.frosts.testplatform.entity.User;
import com.frosts.testplatform.repository.LoginHistoryRepository;
import com.frosts.testplatform.repository.UserRepository;
import com.frosts.testplatform.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username));
        return toProfileResponse(user);
    }

    @Transactional
    public ProfileResponse updateProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username));

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
        return toProfileResponse(user);
    }

    @Transactional(readOnly = true)
    public SecurityInfoResponse getSecurityInfo(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username));

        Page<LoginHistory> recentLogins = loginHistoryRepository.findByUsernameOrderByLoginAtDesc(
                username, PageRequest.of(0, 20));

        return SecurityInfoResponse.builder()
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
