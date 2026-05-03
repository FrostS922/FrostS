package com.frosts.testplatform.service.push;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketPushService implements NotificationPushService {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void pushToUser(Long userId, String type, Map<String, Object> payload) {
        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    Map.of("type", type, "payload", payload)
            );
            log.debug("WebSocket push to user {}: {}", userId, type);
        } catch (Exception e) {
            log.warn("Failed to push notification via WebSocket to user {}: {}", userId, e.getMessage());
        }
    }

    @Override
    public void pushToAll(String type, Map<String, Object> payload) {
        try {
            messagingTemplate.convertAndSend(
                    "/topic/notifications",
                    Map.of("type", type, "payload", payload)
            );
            log.debug("WebSocket push to all: {}", type);
        } catch (Exception e) {
            log.warn("Failed to push notification via WebSocket to all: {}", e.getMessage());
        }
    }
}
