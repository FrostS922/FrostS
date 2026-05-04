package com.frosts.testplatform.service;

import com.frosts.testplatform.dto.notification.*;
import com.frosts.testplatform.entity.Notification;
import com.frosts.testplatform.entity.NotificationPreference;
import com.frosts.testplatform.entity.NotificationRecipient;
import com.frosts.testplatform.entity.User;
import com.frosts.testplatform.event.NotificationEvent;
import com.frosts.testplatform.mapper.NotificationMapper;
import com.frosts.testplatform.repository.NotificationPreferenceRepository;
import com.frosts.testplatform.repository.NotificationRecipientRepository;
import com.frosts.testplatform.repository.NotificationRepository;
import com.frosts.testplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository recipientRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Long userId, String type, Boolean isRead, int page, int size) {
        Page<NotificationRecipient> recipientPage = recipientRepository.findByUserIdWithFilters(
                userId, type, isRead,
                PageRequest.of(page, size, Sort.by("notification.createdAt").descending()));

        return recipientPage.map(notificationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(Long userId) {
        long total = recipientRepository.countByUserIdAndIsReadFalseAndIsDeletedFalse(userId);

        long systemCount = countUnreadByType(userId, "SYSTEM");
        long businessCount = countUnreadByType(userId, "BUSINESS");
        long reminderCount = countUnreadByType(userId, "REMINDER");
        long todoCount = countUnreadByType(userId, "TODO");

        return UnreadCountResponse.builder()
                .total(total)
                .systemCount(systemCount)
                .businessCount(businessCount)
                .reminderCount(reminderCount)
                .todoCount(todoCount)
                .build();
    }

    @Transactional(readOnly = true)
    public NotificationDetailResponse getNotificationDetail(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("通知不存在: " + notificationId));

        NotificationRecipient recipient = recipientRepository
                .findByNotificationIdAndIsDeletedFalse(notificationId).stream()
                .filter(r -> r.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("无权查看此通知"));

        return notificationMapper.toDetailResponse(notification, recipient);
    }

    public void markAsRead(Long notificationId, Long userId) {
        NotificationRecipient recipient = findRecipient(notificationId, userId);
        if (!recipient.getIsRead()) {
            recipient.setIsRead(true);
            recipient.setReadAt(LocalDateTime.now());
            recipientRepository.save(recipient);
        }
    }

    public void markAllAsRead(Long userId) {
        recipientRepository.markAllAsReadByUserId(userId);
    }

    public void toggleStar(Long notificationId, Long userId) {
        NotificationRecipient recipient = findRecipient(notificationId, userId);
        recipient.setIsStarred(!recipient.getIsStarred());
        recipientRepository.save(recipient);
    }

    public void deleteNotification(Long notificationId, Long userId) {
        NotificationRecipient recipient = findRecipient(notificationId, userId);
        recipient.setIsDeleted(true);
        recipientRepository.save(recipient);
    }

    public Notification createNotification(CreateNotificationRequest request) {
        Notification notification = new Notification();
        notification.setTitle(request.getTitle());
        notification.setContent(request.getContent());
        notification.setType(request.getType());
        notification.setCategory(request.getCategory());
        notification.setPriority(request.getPriority() != null ? request.getPriority() : "NORMAL");
        notification.setTargetType(request.getTargetType());
        notification.setTargetId(request.getTargetId());
        notification.setTargetUrl(request.getTargetUrl());
        notification.setIsGlobal(request.getIsGlobal() != null ? request.getIsGlobal() : false);
        notification.setExpiresAt(request.getExpiresAt());
        notification.setIsDeleted(false);

        if (request.getSenderId() != null) {
            User sender = userRepository.findById(request.getSenderId())
                    .orElseThrow(() -> new RuntimeException("发送者不存在: " + request.getSenderId()));
            notification.setSender(sender);
        }

        notification = notificationRepository.save(notification);

        if (Boolean.TRUE.equals(request.getIsGlobal())) {
            List<User> allUsers = userRepository.findAll();
            for (User user : allUsers) {
                createRecipient(notification, user);
            }
        } else if (request.getRecipientIds() != null && !request.getRecipientIds().isEmpty()) {
            for (Long recipientId : request.getRecipientIds()) {
                User user = userRepository.findById(recipientId)
                        .orElseThrow(() -> new RuntimeException("接收者不存在: " + recipientId));
                createRecipient(notification, user);
            }
        }

        return notification;
    }

    public Notification createFromEvent(NotificationEvent event) {
        CreateNotificationRequest request = CreateNotificationRequest.builder()
                .title(event.getTitle())
                .content(event.getContent())
                .type(event.getType())
                .category(event.getCategory())
                .priority(event.getPriority())
                .recipientIds(event.getRecipientIds())
                .targetType(event.getTargetType())
                .targetId(event.getTargetId())
                .targetUrl(event.getTargetUrl())
                .senderId(event.getSenderId())
                .isGlobal(event.isGlobal())
                .build();
        return createNotification(request);
    }

    private NotificationRecipient createRecipient(Notification notification, User user) {
        NotificationRecipient recipient = new NotificationRecipient();
        recipient.setNotification(notification);
        recipient.setUser(user);
        recipient.setIsRead(false);
        recipient.setIsStarred(false);
        recipient.setIsDeleted(false);
        return recipientRepository.save(recipient);
    }

    private NotificationRecipient findRecipient(Long notificationId, Long userId) {
        return recipientRepository.findByNotificationIdAndIsDeletedFalse(notificationId).stream()
                .filter(r -> r.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("通知记录不存在"));
    }

    private long countUnreadByType(Long userId, String type) {
        Page<NotificationRecipient> page = recipientRepository.findByUserIdWithFilters(
                userId, type, false, PageRequest.of(0, 1, Sort.unsorted()));
        return page.getTotalElements();
    }

    @Transactional(readOnly = true)
    public NotificationPreference getPreferenceForUser(Long userId) {
        return preferenceRepository.findByUserId(userId).orElse(null);
    }

    @Transactional(readOnly = true)
    public String getUserEmail(Long userId) {
        return userRepository.findById(userId)
                .map(User::getEmail)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public long countExpiredNotifications() {
        return notificationRepository.countByExpiresAtBeforeAndIsDeletedFalse(LocalDateTime.now());
    }

    @Transactional
    public int cleanExpiredNotifications() {
        List<Notification> expired = notificationRepository.findByExpiresAtBeforeAndIsDeletedFalse(LocalDateTime.now());
        for (Notification notification : expired) {
            notification.setIsDeleted(true);
        }
        notificationRepository.saveAll(expired);
        return expired.size();
    }
}
