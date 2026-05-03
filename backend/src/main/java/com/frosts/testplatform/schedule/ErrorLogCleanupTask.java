package com.frosts.testplatform.schedule;

import com.frosts.testplatform.repository.ErrorLogRepository;
import com.frosts.testplatform.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ErrorLogCleanupTask {

    private final ErrorLogRepository errorLogRepository;
    private final SystemSettingRepository systemSettingRepository;

    private static final int DEFAULT_RETENTION_DAYS = 30;

    @Scheduled(cron = "0 20 3 * * ?")
    @Transactional
    public void cleanupOldErrorLogs() {
        int retentionDays = getRetentionDays();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        log.info("[ERROR_LOG_CLEANUP] 开始清理 {} 之前的错误日志（保留 {} 天）", cutoff, retentionDays);

        try {
            int deleted = errorLogRepository.softDeleteBefore(cutoff);
            if (deleted > 0) {
                log.info("[ERROR_LOG_CLEANUP] 已软删除 {} 条超过 {} 天的错误日志", deleted, retentionDays);
            } else {
                log.info("[ERROR_LOG_CLEANUP] 没有需要清理的记录");
            }
        } catch (Exception e) {
            log.error("[ERROR_LOG_CLEANUP] 清理错误日志失败", e);
        }
    }

    private int getRetentionDays() {
        return systemSettingRepository.findBySettingKeyAndIsDeletedFalse("error_monitor.retention_days")
                .map(s -> {
                    try { return Integer.parseInt(s.getSettingValue()); }
                    catch (NumberFormatException e) { return DEFAULT_RETENTION_DAYS; }
                })
                .orElse(DEFAULT_RETENTION_DAYS);
    }
}
