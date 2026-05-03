package com.frosts.testplatform.service;

import com.frosts.testplatform.entity.LoginHistory;
import com.frosts.testplatform.repository.LoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginHistoryCleanupService {

    private final LoginHistoryRepository loginHistoryRepository;

    private static final int RETENTION_DAYS = 90;

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupOldLoginHistory() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        log.info("[LOGIN_HISTORY_CLEANUP] 开始清理 {} 之前的登录历史记录", cutoff);

        try {
            List<LoginHistory> oldRecords = loginHistoryRepository.findByLoginAtBefore(cutoff);
            if (!oldRecords.isEmpty()) {
                loginHistoryRepository.deleteAll(oldRecords);
                log.info("[LOGIN_HISTORY_CLEANUP] 已清理 {} 条超过 {} 天的登录历史记录", oldRecords.size(), RETENTION_DAYS);
            } else {
                log.info("[LOGIN_HISTORY_CLEANUP] 没有需要清理的记录");
            }
        } catch (Exception e) {
            log.error("[LOGIN_HISTORY_CLEANUP] 清理登录历史失败", e);
        }
    }
}
