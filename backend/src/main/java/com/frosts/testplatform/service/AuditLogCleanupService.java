package com.frosts.testplatform.service;

import com.frosts.testplatform.entity.AuditLog;
import com.frosts.testplatform.repository.AuditLogRepository;
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
public class AuditLogCleanupService {

    private final AuditLogRepository auditLogRepository;

    private static final int RETENTION_DAYS = 180;

    @Scheduled(cron = "0 10 3 * * ?")
    @Transactional
    public void cleanupOldAuditLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        log.info("[AUDIT_LOG_CLEANUP] 开始清理 {} 之前的审计日志记录", cutoff);

        try {
            List<AuditLog> oldRecords = auditLogRepository.findByOperatedAtBefore(cutoff);
            if (!oldRecords.isEmpty()) {
                auditLogRepository.deleteAll(oldRecords);
                log.info("[AUDIT_LOG_CLEANUP] 已清理 {} 条超过 {} 天的审计日志记录", oldRecords.size(), RETENTION_DAYS);
            } else {
                log.info("[AUDIT_LOG_CLEANUP] 没有需要清理的记录");
            }
        } catch (Exception e) {
            log.error("[AUDIT_LOG_CLEANUP] 清理审计日志失败", e);
        }
    }
}
