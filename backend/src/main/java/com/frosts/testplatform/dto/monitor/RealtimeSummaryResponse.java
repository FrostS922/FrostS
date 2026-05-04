package com.frosts.testplatform.dto.monitor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "实时监控概览响应")
public class RealtimeSummaryResponse {

    @Schema(description = "性能快照")
    private PerformanceSnapshot performance;

    @Schema(description = "错误快照")
    private ErrorSnapshot errors;

    @Schema(description = "安全快照")
    private SecuritySnapshot security;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "性能快照")
    public static class PerformanceSnapshot {
        @Schema(description = "性能报告总数", example = "256")
        private long totalReports;
        @Schema(description = "今日性能报告数", example = "18")
        private long todayReports;
        @Schema(description = "各指标统计")
        private List<MetricSnapshot> metrics;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "指标快照")
    public static class MetricSnapshot {
        @Schema(description = "指标名称", example = "LCP")
        private String metricName;
        @Schema(description = "平均值", example = "2.35")
        private double avgValue;
        @Schema(description = "样本数", example = "120")
        private long count;
        @Schema(description = "poor评级数", example = "8")
        private long poorCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "错误快照")
    public static class ErrorSnapshot {
        @Schema(description = "错误总数", example = "45")
        private long totalErrors;
        @Schema(description = "今日错误数", example = "3")
        private long todayErrors;
        @Schema(description = "最近错误列表")
        private List<RecentError> recentErrors;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "最近错误")
    public static class RecentError {
        @Schema(description = "错误信息", example = "TypeError: Cannot read properties of undefined")
        private String errorMessage;
        @Schema(description = "错误分类", example = "CODE")
        private String category;
        @Schema(description = "发生时间", example = "2026-05-04 09:30:00")
        private String createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "安全快照")
    public static class SecuritySnapshot {
        @Schema(description = "今日登录成功次数", example = "15")
        private long todayLoginSuccesses;
        @Schema(description = "今日登录失败次数", example = "3")
        private long todayLoginFailures;
        @Schema(description = "今日异常IP数", example = "2")
        private long todayAnomalousIps;
        @Schema(description = "锁定账户数", example = "0")
        private long lockedAccounts;
        @Schema(description = "封禁IP数", example = "1")
        private long bannedIps;
    }
}
