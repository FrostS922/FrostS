package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.dto.notification.*;
import com.frosts.testplatform.entity.User;
import com.frosts.testplatform.security.CustomUserDetailsService;
import com.frosts.testplatform.service.NotificationPreferenceService;
import com.frosts.testplatform.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationPreferenceService preferenceService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            Authentication authentication,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId(authentication);
        Page<NotificationResponse> notifications = notificationService.getNotifications(userId, type, isRead, page, size);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadCount(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationDetailResponse>> getNotificationDetail(
            @PathVariable Long id, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(notificationService.getNotificationDetail(id, userId)));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/{id}/star")
    public ResponseEntity<ApiResponse<Void>> toggleStar(@PathVariable Long id, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        notificationService.toggleStar(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable Long id, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        notificationService.deleteNotification(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<com.frosts.testplatform.entity.Notification>> createNotification(
            @Valid @RequestBody CreateNotificationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.createNotification(request)));
    }

    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> getPreference(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(preferenceService.getPreference(userId)));
    }

    @PutMapping("/preferences")
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> updatePreference(
            Authentication authentication,
            @RequestBody UpdateNotificationPreferenceRequest request) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(preferenceService.updatePreference(userId, request)));
    }

    private Long getCurrentUserId(Authentication authentication) {
        CustomUserDetailsService.CustomUserDetails userDetails =
                (CustomUserDetailsService.CustomUserDetails) authentication.getPrincipal();
        return userDetails.getUser().getId();
    }
}
