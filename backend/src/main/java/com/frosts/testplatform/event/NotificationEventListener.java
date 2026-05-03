package com.frosts.testplatform.event;

import com.frosts.testplatform.dto.notification.UnreadCountResponse;
import com.frosts.testplatform.service.NotificationService;
import com.frosts.testplatform.service.push.NotificationPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final NotificationPushService pushService;
    private final JavaMailSender mailSender;

    @Async
    @EventListener
    public void handleNotificationEvent(NotificationEvent event) {
        try {
            var notification = notificationService.createFromEvent(event);
            log.info("Notification event processed: type={}, category={}, title={}",
                    event.getType(), event.getCategory(), event.getTitle());

            Map<String, Object> payload = new HashMap<>();
            payload.put("id", notification.getId());
            payload.put("title", notification.getTitle());
            payload.put("type", notification.getType());
            payload.put("category", event.getCategory());
            payload.put("priority", notification.getPriority());
            payload.put("targetUrl", notification.getTargetUrl());
            payload.put("createdAt", notification.getCreatedAt().toString());

            if (Boolean.TRUE.equals(event.isGlobal())) {
                pushService.pushToAll("NEW_NOTIFICATION", payload);
            } else if (event.getRecipientIds() != null) {
                for (Long userId : event.getRecipientIds()) {
                    pushService.pushToUser(userId, "NEW_NOTIFICATION", payload);
                    sendEmailIfNeeded(userId, event);
                }
            }
        } catch (Exception e) {
            log.error("Failed to process notification event: {}", event.getTitle(), e);
        }
    }

    private void sendEmailIfNeeded(Long userId, NotificationEvent event) {
        try {
            var preference = notificationService.getPreferenceForUser(userId);
            if (preference != null
                    && Boolean.TRUE.equals(preference.getReceiveChannels().get("email"))
                    && Boolean.TRUE.equals(preference.getTypeSettings().get(event.getType()))) {

                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(getUserEmail(userId));
                message.setSubject("[FrostS] " + event.getTitle());
                message.setText(event.getContent() != null ? event.getContent() : event.getTitle());
                mailSender.send(message);
                log.debug("Email notification sent to user {}", userId);
            }
        } catch (Exception e) {
            log.warn("Failed to send email notification to user {}: {}", userId, e.getMessage());
        }
    }

    private String getUserEmail(Long userId) {
        return notificationService.getUserEmail(userId);
    }
}
