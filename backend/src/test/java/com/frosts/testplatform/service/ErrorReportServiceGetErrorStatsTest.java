package com.frosts.testplatform.service;

import com.frosts.testplatform.dto.report.ReportExportDataResponse;
import com.frosts.testplatform.repository.ErrorLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrorReportServiceGetErrorStatsTest {

    @Mock
    private ErrorLogRepository errorLogRepository;

    @Mock
    private com.frosts.testplatform.repository.UserRepository userRepository;

    @Mock
    private SystemSettingService systemSettingService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private com.frosts.testplatform.service.push.NotificationPushService notificationPushService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Mock
    private com.frosts.testplatform.mapper.ErrorLogMapper errorLogMapper;

    @InjectMocks
    private ErrorReportService errorReportService;

    @Test
    void getErrorStatsReturnsCorrectData() {
        when(errorLogRepository.countByIsDeletedFalse()).thenReturn(50L);
        when(errorLogRepository.countByCreatedAtAfterAndIsDeletedFalse(any(LocalDateTime.class))).thenReturn(5L);

        Object[] row1 = new Object[]{"NETWORK", 20L};
        Object[] row2 = new Object[]{"CODE", 15L};
        Object[] row3 = new Object[]{"AUTH", 10L};
        Object[] row4 = new Object[]{"OTHER", 5L};
        List<Object[]> categoryCounts = new ArrayList<>();
        categoryCounts.add(row1);
        categoryCounts.add(row2);
        categoryCounts.add(row3);
        categoryCounts.add(row4);
        when(errorLogRepository.countByCategoryInRange(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(categoryCounts);

        LocalDateTime startTime = LocalDateTime.of(2026, 4, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 5, 4, 0, 0);

        ReportExportDataResponse.ErrorStats stats = errorReportService.getErrorStats(startTime, endTime);

        assertThat(stats.getTotalErrors()).isEqualTo(50L);
        assertThat(stats.getTodayErrors()).isEqualTo(5L);
        assertThat(stats.getTopCategories()).hasSize(3);
        assertThat(stats.getTopCategories().get(0).getCategory()).isEqualTo("NETWORK");
        assertThat(stats.getTopCategories().get(0).getCount()).isEqualTo(20L);
        assertThat(stats.getTopCategories().get(1).getCategory()).isEqualTo("CODE");
        assertThat(stats.getTopCategories().get(2).getCategory()).isEqualTo("AUTH");
    }

    @Test
    void getErrorStatsWithEmptyCategories() {
        when(errorLogRepository.countByIsDeletedFalse()).thenReturn(0L);
        when(errorLogRepository.countByCreatedAtAfterAndIsDeletedFalse(any(LocalDateTime.class))).thenReturn(0L);
        when(errorLogRepository.countByCategoryInRange(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());

        LocalDateTime startTime = LocalDateTime.of(2026, 4, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 5, 4, 0, 0);

        ReportExportDataResponse.ErrorStats stats = errorReportService.getErrorStats(startTime, endTime);

        assertThat(stats.getTotalErrors()).isZero();
        assertThat(stats.getTodayErrors()).isZero();
        assertThat(stats.getTopCategories()).isEmpty();
    }

    @Test
    void getErrorStatsLimitsToTop3Categories() {
        when(errorLogRepository.countByIsDeletedFalse()).thenReturn(100L);
        when(errorLogRepository.countByCreatedAtAfterAndIsDeletedFalse(any(LocalDateTime.class))).thenReturn(10L);

        List<Object[]> categoryCounts = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            categoryCounts.add(new Object[]{"CAT_" + i, (long) (6 - i)});
        }
        when(errorLogRepository.countByCategoryInRange(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(categoryCounts);

        LocalDateTime startTime = LocalDateTime.of(2026, 4, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 5, 4, 0, 0);

        ReportExportDataResponse.ErrorStats stats = errorReportService.getErrorStats(startTime, endTime);

        assertThat(stats.getTopCategories()).hasSize(3);
        assertThat(stats.getTopCategories().get(0).getCategory()).isEqualTo("CAT_0");
        assertThat(stats.getTopCategories().get(0).getCount()).isEqualTo(6L);
    }
}
