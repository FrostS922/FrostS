package com.frosts.testplatform.service;

import com.frosts.testplatform.dto.ProjectStatisticsDTO;
import com.frosts.testplatform.entity.Defect;
import com.frosts.testplatform.entity.Requirement;
import com.frosts.testplatform.entity.TestCase;
import com.frosts.testplatform.entity.TestPlan;
import com.frosts.testplatform.repository.DefectRepository;
import com.frosts.testplatform.repository.RequirementRepository;
import com.frosts.testplatform.repository.TestCaseRepository;
import com.frosts.testplatform.repository.TestPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

    private final RequirementRepository requirementRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestPlanRepository testPlanRepository;
    private final DefectRepository defectRepository;

    public ProjectStatisticsDTO getProjectStatistics(Long projectId) {
        ProjectStatisticsDTO stats = new ProjectStatisticsDTO();

        // 基础计数
        stats.setTotalRequirements(requirementRepository.countByProjectId(projectId));
        stats.setTotalTestCases(testCaseRepository.countByProjectId(projectId));
        stats.setTotalTestPlans(testPlanRepository.countByProjectId(projectId));
        stats.setTotalDefects(defectRepository.countByProjectId(projectId));

        // 需求分布
        List<Requirement> requirements = requirementRepository.findByProjectId(projectId);
        stats.setRequirementStatusDistribution(
                requirements.stream().collect(Collectors.groupingBy(
                        r -> r.getStatus() != null ? r.getStatus() : "UNKNOWN",
                        Collectors.counting()
                ))
        );
        stats.setRequirementPriorityDistribution(
                requirements.stream().collect(Collectors.groupingBy(
                        r -> r.getPriority() != null ? r.getPriority() : "UNKNOWN",
                        Collectors.counting()
                ))
        );

        // 测试用例分布
        List<TestCase> testCases = testCaseRepository.findByProjectId(projectId);

        // 需求覆盖率（需在 testCases 声明后计算）
        long coveredRequirements = requirements.stream()
                .filter(r -> testCases.stream().anyMatch(tc -> tc.getRequirement() != null && tc.getRequirement().getId().equals(r.getId())))
                .count();
        stats.setRequirementCoverageRate(requirements.isEmpty() ? 0.0 :
                BigDecimal.valueOf(coveredRequirements * 100.0 / requirements.size())
                        .setScale(2, RoundingMode.HALF_UP).doubleValue()
        );

        long uncoveredCount = requirements.size() - coveredRequirements;
        Map<String, Long> coverageDist = new java.util.LinkedHashMap<>();
        coverageDist.put("已覆盖", coveredRequirements);
        coverageDist.put("未覆盖", uncoveredCount);
        stats.setRequirementCoverageDistribution(coverageDist);

        stats.setTestCaseStatusDistribution(
                testCases.stream().collect(Collectors.groupingBy(
                        t -> t.getStatus() != null ? t.getStatus() : "UNKNOWN",
                        Collectors.counting()
                ))
        );
        stats.setTestCasePriorityDistribution(
                testCases.stream().collect(Collectors.groupingBy(
                        t -> t.getPriority() != null ? t.getPriority() : "UNKNOWN",
                        Collectors.counting()
                ))
        );
        stats.setTestCaseTypeDistribution(
                testCases.stream().collect(Collectors.groupingBy(
                        t -> t.getType() != null ? t.getType() : "UNKNOWN",
                        Collectors.counting()
                ))
        );
        long automatedCount = testCases.stream().filter(t -> Boolean.TRUE.equals(t.getIsAutomated())).count();
        stats.setAutomatedTestCases(automatedCount);
        stats.setAutomationRate(testCases.isEmpty() ? 0.0 :
                BigDecimal.valueOf(automatedCount * 100.0 / testCases.size())
                        .setScale(2, RoundingMode.HALF_UP).doubleValue()
        );

        // 测试计划分布
        List<TestPlan> testPlans = testPlanRepository.findByProjectId(projectId);
        stats.setTestPlanStatusDistribution(
                testPlans.stream().collect(Collectors.groupingBy(
                        p -> p.getStatus() != null ? p.getStatus() : "UNKNOWN",
                        Collectors.counting()
                ))
        );
        double avgPassRate = testPlans.stream()
                .filter(p -> p.getTotalCases() != null && p.getTotalCases() > 0)
                .mapToDouble(p -> p.getPassedCases() != null ?
                        p.getPassedCases() * 100.0 / p.getTotalCases() : 0.0)
                .average().orElse(0.0);
        stats.setAveragePassRate(BigDecimal.valueOf(avgPassRate)
                .setScale(2, RoundingMode.HALF_UP).doubleValue());

        // 缺陷分布
        List<Defect> defects = defectRepository.findByProjectId(projectId);

        stats.setDefectStatusDistribution(
                defects.stream().collect(Collectors.groupingBy(
                        d -> d.getStatus() != null ? d.getStatus() : "UNKNOWN",
                        Collectors.counting()
                ))
        );
        stats.setDefectSeverityDistribution(
                defects.stream().collect(Collectors.groupingBy(
                        d -> d.getSeverity() != null ? d.getSeverity() : "UNKNOWN",
                        Collectors.counting()
                ))
        );
        stats.setDefectPriorityDistribution(
                defects.stream().collect(Collectors.groupingBy(
                        d -> d.getPriority() != null ? d.getPriority() : "UNKNOWN",
                        Collectors.counting()
                ))
        );
        stats.setDefectTypeDistribution(
                defects.stream().collect(Collectors.groupingBy(
                        d -> d.getType() != null ? d.getType() : "UNKNOWN",
                        Collectors.counting()
                ))
        );
        stats.setReopenedDefects(defects.stream().filter(d -> d.getReopenCount() != null && d.getReopenCount() > 0).count());

        long reqCount = stats.getTotalRequirements();
        stats.setDefectDensity(reqCount > 0 ?
                BigDecimal.valueOf(stats.getTotalDefects() * 1.0 / reqCount)
                        .setScale(2, RoundingMode.HALF_UP).doubleValue() : 0.0
        );

        // 趋势数据（最近30天）
        stats.setDefectTrend(generateDefectTrend(projectId, 30));
        stats.setTestExecutionTrend(generateTestExecutionTrend(projectId, 30));
        stats.setRequirementCompletionTrend(generateRequirementTrend(projectId, 30));

        return stats;
    }

    private List<ProjectStatisticsDTO.TrendData> generateDefectTrend(Long projectId, int days) {
        List<ProjectStatisticsDTO.TrendData> trend = new ArrayList<>();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            List<Defect> dayDefects = defectRepository.findByProjectIdAndCreatedAtBetween(projectId, dayStart, dayEnd);

            ProjectStatisticsDTO.TrendData data = new ProjectStatisticsDTO.TrendData();
            data.setDate(date.format(formatter));
            data.setCreated((long) dayDefects.size());
            data.setResolved(dayDefects.stream().filter(d -> d.getResolvedAt() != null &&
                    !d.getResolvedAt().isBefore(dayStart) && d.getResolvedAt().isBefore(dayEnd)).count());
            data.setClosed(dayDefects.stream().filter(d -> d.getClosedAt() != null &&
                    !d.getClosedAt().isBefore(dayStart) && d.getClosedAt().isBefore(dayEnd)).count());
            trend.add(data);
        }
        return trend;
    }

    private List<ProjectStatisticsDTO.TrendData> generateTestExecutionTrend(Long projectId, int days) {
        List<ProjectStatisticsDTO.TrendData> trend = new ArrayList<>();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            ProjectStatisticsDTO.TrendData data = new ProjectStatisticsDTO.TrendData();
            data.setDate(date.format(formatter));
            // Simplified - in real scenario would query test execution records
            data.setCreated(0L);
            data.setResolved(0L);
            data.setClosed(0L);
            trend.add(data);
        }
        return trend;
    }

    private List<ProjectStatisticsDTO.TrendData> generateRequirementTrend(Long projectId, int days) {
        List<ProjectStatisticsDTO.TrendData> trend = new ArrayList<>();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            List<Requirement> dayReqs = requirementRepository.findByProjectId(projectId).stream()
                    .filter(r -> r.getCreatedAt() != null && !r.getCreatedAt().isBefore(dayStart) && r.getCreatedAt().isBefore(dayEnd))
                    .collect(Collectors.toList());

            ProjectStatisticsDTO.TrendData data = new ProjectStatisticsDTO.TrendData();
            data.setDate(date.format(formatter));
            data.setCreated((long) dayReqs.size());
            data.setResolved(dayReqs.stream().filter(r -> "COMPLETED".equals(r.getStatus())).count());
            data.setClosed(0L);
            trend.add(data);
        }
        return trend;
    }
}
