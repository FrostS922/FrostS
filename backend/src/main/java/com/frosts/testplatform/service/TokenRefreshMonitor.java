package com.frosts.testplatform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class TokenRefreshMonitor {

    private static final int WARN_THRESHOLD_PER_MINUTE = 5;
    private static final int ALERT_THRESHOLD_PER_MINUTE = 10;
    private static final int BAN_THRESHOLD_PER_MINUTE = 20;

    private final SecurityAlertService securityAlertService;

    private final Map<String, RefreshCounter> refreshCounts = new ConcurrentHashMap<>();

    public TokenRefreshMonitor(SecurityAlertService securityAlertService) {
        this.securityAlertService = securityAlertService;
    }

    public RefreshCheckResult recordRefresh(String username, String clientIp) {
        RefreshCounter counter = refreshCounts.computeIfAbsent(username, k -> new RefreshCounter());
        int count = counter.increment();

        if (count >= BAN_THRESHOLD_PER_MINUTE) {
            log.error("[TOKEN_REFRESH_BAN] 用户 {} 在1分钟内刷新Token {} 次, 客户端IP: {}, 触发自动封禁",
                    username, count, clientIp);
            securityAlertService.autoBanUser(username, count, clientIp);
            return RefreshCheckResult.BANNED;
        } else if (count >= ALERT_THRESHOLD_PER_MINUTE) {
            log.warn("[TOKEN_REFRESH_ALERT] 用户 {} 在1分钟内刷新Token {} 次, 客户端IP: {}, 可能存在Token滥用或异常使用模式",
                    username, count, clientIp);
            if (count == ALERT_THRESHOLD_PER_MINUTE) {
                securityAlertService.sendTokenAbuseAlert(username, count, clientIp);
            }
            return RefreshCheckResult.ALERT;
        } else if (count >= WARN_THRESHOLD_PER_MINUTE) {
            log.warn("[TOKEN_REFRESH_WARN] 用户 {} 在1分钟内刷新Token {} 次, 客户端IP: {}",
                    username, count, clientIp);
            return RefreshCheckResult.WARN;
        } else {
            log.debug("[TOKEN_REFRESH] 用户 {} 刷新Token, 第 {} 次, 客户端IP: {}",
                    username, count, clientIp);
            return RefreshCheckResult.NORMAL;
        }
    }

    public boolean isUserBanned(String username) {
        RefreshCounter counter = refreshCounts.get(username);
        return counter != null && counter.getCount() >= BAN_THRESHOLD_PER_MINUTE;
    }

    public void cleanup() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(1);
        refreshCounts.entrySet().removeIf(entry -> {
            if (entry.getValue().isBefore(threshold)) {
                if (entry.getValue().getCount() >= WARN_THRESHOLD_PER_MINUTE) {
                    log.info("[TOKEN_REFRESH_SUMMARY] 用户 {} 过去1分钟共刷新 {} 次",
                            entry.getKey(), entry.getValue().getCount());
                }
                return true;
            }
            return false;
        });
    }

    public enum RefreshCheckResult {
        NORMAL, WARN, ALERT, BANNED
    }

    private static class RefreshCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile LocalDateTime windowStart = LocalDateTime.now();

        int increment() {
            LocalDateTime now = LocalDateTime.now();
            if (windowStart.isBefore(now.minusMinutes(1))) {
                synchronized (this) {
                    if (windowStart.isBefore(now.minusMinutes(1))) {
                        windowStart = now;
                        count.set(0);
                    }
                }
            }
            return count.incrementAndGet();
        }

        int getCount() {
            return count.get();
        }

        boolean isBefore(LocalDateTime threshold) {
            return windowStart.isBefore(threshold);
        }
    }
}
