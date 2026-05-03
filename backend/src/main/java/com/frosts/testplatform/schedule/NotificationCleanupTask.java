package com.frosts.testplatform.schedule;

import com.frosts.testplatform.repository.RefreshTokenRepository;
import com.frosts.testplatform.service.NotificationService;
import com.frosts.testplatform.service.TokenRefreshMonitor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupTask {

    private final NotificationService notificationService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenRefreshMonitor tokenRefreshMonitor;

    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredNotifications() {
        try {
            int cleaned = notificationService.cleanExpiredNotifications();
            if (cleaned > 0) {
                log.info("Cleaned up {} expired notifications", cleaned);
            }
        } catch (Exception e) {
            log.error("Failed to clean expired notifications", e);
        }
    }

    @Scheduled(cron = "0 0 9 * * ?")
    public void checkOverdueReminders() {
        log.info("Overdue reminder check executed at 09:00");
    }

    @Scheduled(cron = "0 30 2 * * ?")
    public void cleanExpiredRefreshTokens() {
        try {
            LocalDateTime now = LocalDateTime.now();
            refreshTokenRepository.deleteByExpiryDateBefore(now);
            log.info("Cleaned up expired refresh tokens before {}", now);
        } catch (Exception e) {
            log.error("Failed to clean expired refresh tokens", e);
        }
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void cleanupRefreshMonitor() {
        try {
            tokenRefreshMonitor.cleanup();
        } catch (Exception e) {
            log.error("Failed to cleanup refresh monitor", e);
        }
    }
}
