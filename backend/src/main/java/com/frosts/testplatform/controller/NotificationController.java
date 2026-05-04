package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.dto.notification.*;
import com.frosts.testplatform.entity.User;
import com.frosts.testplatform.security.CustomUserDetailsService;
import com.frosts.testplatform.service.NotificationPreferenceService;
import com.frosts.testplatform.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "通知管理", description = "通知消息与偏好设置")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationPreferenceService preferenceService;

    @GetMapping
    @Operation(summary = "分页查询通知列表")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            Authentication authentication,
            @RequestParam(required = false) @Parameter(description = "通知类型") String type,
            @RequestParam(required = false) @Parameter(description = "是否已读") Boolean isRead,
            @RequestParam(defaultValue = "0") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "10") @Parameter(description = "每页数量") int size) {
        Long userId = getCurrentUserId(authentication);
        Page<NotificationResponse> notifications = notificationService.getNotifications(userId, type, isRead, page, size);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "获取未读通知数量")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadCount(userId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取通知详情")
    public ResponseEntity<ApiResponse<NotificationDetailResponse>> getNotificationDetail(
            @PathVariable @Parameter(description = "通知ID") Long id, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(notificationService.getNotificationDetail(id, userId)));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记通知已读")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable @Parameter(description = "通知ID") Long id, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/read-all")
    @Operation(summary = "全部标记已读")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/{id}/star")
    @Operation(summary = "切换通知星标状态")
    public ResponseEntity<ApiResponse<Void>> toggleStar(@PathVariable @Parameter(description = "通知ID") Long id, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        notificationService.toggleStar(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除通知")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable @Parameter(description = "通知ID") Long id, Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        notificationService.deleteNotification(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "创建通知")
    public ResponseEntity<ApiResponse<com.frosts.testplatform.entity.Notification>> createNotification(
            @Valid @RequestBody CreateNotificationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.createNotification(request)));
    }

    @GetMapping("/preferences")
    @Operation(summary = "获取通知偏好设置")
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> getPreference(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(preferenceService.getPreference(userId)));
    }

    @PutMapping("/preferences")
    @Operation(summary = "更新通知偏好设置")
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
