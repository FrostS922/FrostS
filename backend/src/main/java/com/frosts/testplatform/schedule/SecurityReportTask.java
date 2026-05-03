package com.frosts.testplatform.schedule;

import com.frosts.testplatform.repository.SystemSettingRepository;
import com.frosts.testplatform.service.SecurityWeeklyReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityReportTask {

    private final SecurityWeeklyReportService securityWeeklyReportService;
    private final SystemSettingRepository systemSettingRepository;

    @Scheduled(cron = "0 0 9 ? * MON")
    public void sendWeeklySecurityReport() {
        boolean enabled = systemSettingRepository
                .findBySettingKeyAndIsDeletedFalse("security.weekly_report.enabled")
                .map(s -> "true".equalsIgnoreCase(s.getSettingValue()))
                .orElse(true);
        if (enabled) {
            log.info("[WEEKLY_REPORT] 开始生成安全周报");
            securityWeeklyReportService.generateAndSendWeeklyReport();
        }
    }
}
