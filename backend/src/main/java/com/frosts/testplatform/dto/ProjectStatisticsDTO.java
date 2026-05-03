package com.frosts.testplatform.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ProjectStatisticsDTO {

    // 概览统计
    private Long totalRequirements;
    private Long totalTestCases;
    private Long totalTestPlans;
    private Long totalDefects;

    // 需求统计
    private Map<String, Long> requirementStatusDistribution;
    private Map<String, Long> requirementPriorityDistribution;
    private Double requirementCoverageRate;
    private Map<String, Long> requirementCoverageDistribution;

    // 测试用例统计
    private Map<String, Long> testCaseStatusDistribution;
    private Map<String, Long> testCasePriorityDistribution;
    private Map<String, Long> testCaseTypeDistribution;
    private Long automatedTestCases;
    private Double automationRate;

    // 测试计划统计
    private Map<String, Long> testPlanStatusDistribution;
    private Double averagePassRate;

    // 缺陷统计
    private Map<String, Long> defectStatusDistribution;
    private Map<String, Long> defectSeverityDistribution;
    private Map<String, Long> defectPriorityDistribution;
    private Map<String, Long> defectTypeDistribution;
    private Long reopenedDefects;
    private Double defectDensity; // 缺陷密度 = 缺陷数 / 需求数

    // 趋势数据
    private List<TrendData> defectTrend; // 缺陷趋势（按日期）
    private List<TrendData> testExecutionTrend; // 测试执行趋势
    private List<TrendData> requirementCompletionTrend; // 需求完成趋势

    @Data
    public static class TrendData {
        private String date;
        private Long created;
        private Long resolved;
        private Long closed;
    }
}
