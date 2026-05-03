package com.frosts.testplatform.service;

import com.frosts.testplatform.dto.testplan.AddTestCaseRequest;
import com.frosts.testplatform.dto.testplan.AssignTestCaseRequest;
import com.frosts.testplatform.dto.testplan.BatchAddTestCasesRequest;
import com.frosts.testplatform.dto.testplan.BatchAssignTestCasesRequest;
import com.frosts.testplatform.dto.testplan.BatchExecuteTestCasesRequest;
import com.frosts.testplatform.dto.testplan.BatchRemoveTestCasesRequest;
import com.frosts.testplatform.dto.testplan.ExecuteTestCaseRequest;
import com.frosts.testplatform.dto.testplan.TestCaseSummary;
import com.frosts.testplatform.dto.testplan.TestPlanCaseResponse;
import com.frosts.testplatform.entity.TestCase;
import com.frosts.testplatform.entity.TestCaseModule;
import com.frosts.testplatform.entity.TestPlan;
import com.frosts.testplatform.entity.TestPlanCase;
import com.frosts.testplatform.event.NotificationEvent;
import com.frosts.testplatform.repository.TestCaseRepository;
import com.frosts.testplatform.repository.TestPlanCaseRepository;
import com.frosts.testplatform.repository.TestPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class TestPlanService {

    private final TestPlanRepository testPlanRepository;
    private final TestPlanCaseRepository testPlanCaseRepository;
    private final TestCaseRepository testCaseRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public Page<TestPlan> getTestPlansByProject(Long projectId, Pageable pageable) {
        return testPlanRepository.findByProjectId(projectId, pageable);
    }

    @Transactional(readOnly = true)
    public TestPlan getTestPlanById(Long id) {
        return testPlanRepository.findById(id).orElseThrow(() -> new RuntimeException("测试计划不存在: " + id));
    }

    @Transactional(readOnly = true)
    public List<TestPlanCaseResponse> getTestPlanCases(Long testPlanId) {
        List<TestPlanCase> cases = testPlanCaseRepository.findByTestPlanId(testPlanId);
        return cases.stream().map(this::toTestPlanCaseResponse).toList();
    }

    public TestPlan createTestPlan(TestPlan testPlan) {
        String planNumber = generatePlanNumber(testPlan.getProject().getId());
        testPlan.setPlanNumber(planNumber);
        if (testPlan.getStatus() == null) {
            testPlan.setStatus("DRAFT");
        }
        if (testPlan.getProgress() == null) {
            testPlan.setProgress(new java.math.BigDecimal("0.00"));
        }
        if (testPlan.getTotalCases() == null) testPlan.setTotalCases(0);
        if (testPlan.getPassedCases() == null) testPlan.setPassedCases(0);
        if (testPlan.getFailedCases() == null) testPlan.setFailedCases(0);
        if (testPlan.getBlockedCases() == null) testPlan.setBlockedCases(0);
        if (testPlan.getNotRunCases() == null) testPlan.setNotRunCases(0);
        return testPlanRepository.save(testPlan);
    }

    public TestPlan updateTestPlan(Long id, TestPlan testPlanDetails) {
        TestPlan testPlan = testPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("测试计划不存在: " + id));

        testPlan.setName(testPlanDetails.getName());
        testPlan.setDescription(testPlanDetails.getDescription());
        testPlan.setStartDate(testPlanDetails.getStartDate());
        testPlan.setEndDate(testPlanDetails.getEndDate());
        testPlan.setActualStartDate(testPlanDetails.getActualStartDate());
        testPlan.setActualEndDate(testPlanDetails.getActualEndDate());
        testPlan.setStatus(testPlanDetails.getStatus());
        testPlan.setOwner(testPlanDetails.getOwner());
        testPlan.setEnvironment(testPlanDetails.getEnvironment());
        testPlan.setMilestone(testPlanDetails.getMilestone());
        testPlan.setProgress(testPlanDetails.getProgress());
        testPlan.setRisk(testPlanDetails.getRisk());
        testPlan.setEntryCriteria(testPlanDetails.getEntryCriteria());
        testPlan.setExitCriteria(testPlanDetails.getExitCriteria());
        testPlan.setTestStrategy(testPlanDetails.getTestStrategy());
        testPlan.setScope(testPlanDetails.getScope());

        String oldStatus = testPlan.getStatus();
        String newStatus = testPlanDetails.getStatus();

        if (newStatus != null && !newStatus.equals(oldStatus)) {
            eventPublisher.publishEvent(NotificationEvent.builder()
                    .source(this)
                    .type("BUSINESS")
                    .category("PLAN_STATUS_CHANGED")
                    .title("测试计划状态变更: " + testPlan.getName())
                    .content("测试计划 " + testPlan.getPlanNumber() + " 状态从 " + oldStatus + " 变更为 " + newStatus)
                    .targetType("TEST_PLAN")
                    .targetId(id)
                    .targetUrl("/projects/" + testPlan.getProject().getId() + "/testplans")
                    .build());
        }

        return testPlanRepository.save(testPlan);
    }

    public void deleteTestPlan(Long id) {
        TestPlan testPlan = testPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("测试计划不存在: " + id));
        testPlan.setIsDeleted(true);
        testPlanRepository.save(testPlan);
    }

    public TestPlanCaseResponse addTestCaseToPlan(Long testPlanId, AddTestCaseRequest request) {
        TestPlan testPlan = testPlanRepository.findById(testPlanId)
                .orElseThrow(() -> new RuntimeException("测试计划不存在: " + testPlanId));
        TestCase testCase = testCaseRepository.findById(request.getTestCaseId())
                .orElseThrow(() -> new RuntimeException("测试用例不存在: " + request.getTestCaseId()));

        TestPlanCase testPlanCase = new TestPlanCase();
        testPlanCase.setTestPlan(testPlan);
        testPlanCase.setTestCase(testCase);
        testPlanCase.setStatus("NOT_RUN");
        testPlanCase.setPriority(request.getPriority());
        testPlanCase.setAssignedTo(request.getAssignedTo());

        return toTestPlanCaseResponse(testPlanCaseRepository.save(testPlanCase));
    }

    public TestPlanCaseResponse executeTestCase(Long testPlanCaseId, ExecuteTestCaseRequest request) {
        TestPlanCase testPlanCase = testPlanCaseRepository.findById(testPlanCaseId)
                .orElseThrow(() -> new RuntimeException("测试用例不存在: " + testPlanCaseId));

        String oldStatus = testPlanCase.getStatus();
        testPlanCase.setStatus(request.getStatus());
        testPlanCase.setActualResult(request.getActualResult());
        testPlanCase.setExecutedBy(request.getExecutedBy());
        testPlanCase.setExecutedAt(LocalDateTime.now());

        if (request.getDefectId() != null) testPlanCase.setDefectId(request.getDefectId());
        if (request.getDefectLink() != null) testPlanCase.setDefectLink(request.getDefectLink());
        if (request.getEvidence() != null) testPlanCase.setEvidence(request.getEvidence());

        if ("BLOCKED".equals(request.getStatus())) {
            testPlanCase.setIsBlocked(true);
            testPlanCase.setBlockReason(request.getBlockReason());
        } else {
            testPlanCase.setIsBlocked(false);
            testPlanCase.setBlockReason(null);
        }

        if (oldStatus != null && !oldStatus.equals("NOT_RUN") && !oldStatus.equals(request.getStatus())) {
            testPlanCase.setRetryCount(testPlanCase.getRetryCount() + 1);
        }

        updatePlanStatistics(testPlanCase.getTestPlan().getId());

        return toTestPlanCaseResponse(testPlanCaseRepository.save(testPlanCase));
    }

    public void updatePlanStatistics(Long testPlanId) {
        TestPlan testPlan = testPlanRepository.findById(testPlanId)
                .orElseThrow(() -> new RuntimeException("测试计划不存在: " + testPlanId));

        List<TestPlanCase> cases = testPlanCaseRepository.findByTestPlanId(testPlanId);
        int total = cases.size();
        int passed = (int) cases.stream().filter(c -> "PASSED".equals(c.getStatus())).count();
        int failed = (int) cases.stream().filter(c -> "FAILED".equals(c.getStatus())).count();
        int blocked = (int) cases.stream().filter(c -> "BLOCKED".equals(c.getStatus())).count();
        int notRun = (int) cases.stream().filter(c -> c.getStatus() == null || "NOT_RUN".equals(c.getStatus())).count();

        testPlan.setTotalCases(total);
        testPlan.setPassedCases(passed);
        testPlan.setFailedCases(failed);
        testPlan.setBlockedCases(blocked);
        testPlan.setNotRunCases(notRun);

        if (total > 0) {
            BigDecimal progress = new BigDecimal((total - notRun) * 100)
                    .divide(new BigDecimal(total), 2, java.math.RoundingMode.HALF_UP);
            testPlan.setProgress(progress);
        }

        testPlanRepository.save(testPlan);
    }

    public long countByStatus(Long testPlanId, String status) {
        return testPlanCaseRepository.countByTestPlanIdAndStatus(testPlanId, status);
    }

    private String generatePlanNumber(Long projectId) {
        String prefix = "TP-" + String.format("%04d", projectId) + "-";
        long count = testPlanRepository.count() + 1;
        return prefix + String.format("%06d", count);
    }

    public List<TestPlanCaseResponse> batchAddTestCases(Long testPlanId, BatchAddTestCasesRequest request) {
        TestPlan testPlan = testPlanRepository.findById(testPlanId)
                .orElseThrow(() -> new RuntimeException("测试计划不存在: " + testPlanId));

        List<Long> existingCaseIds = testPlanCaseRepository.findTestCaseIdsByTestPlanId(testPlanId);
        List<Long> newCaseIds = request.getTestCaseIds().stream()
                .filter(id -> !existingCaseIds.contains(id))
                .toList();

        List<TestPlanCase> saved = new ArrayList<>();
        for (Long caseId : newCaseIds) {
            TestPlanCase tpc = new TestPlanCase();
            tpc.setTestPlan(testPlan);
            TestCase testCase = testCaseRepository.findById(caseId)
                    .orElseThrow(() -> new RuntimeException("测试用例不存在: " + caseId));
            tpc.setTestCase(testCase);
            tpc.setStatus("NOT_RUN");
            saved.add(testPlanCaseRepository.save(tpc));
        }
        updatePlanStatistics(testPlanId);
        return saved.stream().map(this::toTestPlanCaseResponse).toList();
    }

    public void batchRemoveTestCases(Long testPlanId, BatchRemoveTestCasesRequest request) {
        List<TestPlanCase> cases = testPlanCaseRepository.findByIdIn(request.getPlanCaseIds());
        for (TestPlanCase tpc : cases) {
            tpc.setIsDeleted(true);
        }
        testPlanCaseRepository.saveAll(cases);
        updatePlanStatistics(testPlanId);
    }

    public List<TestPlanCaseResponse> batchExecuteTestCases(BatchExecuteTestCasesRequest request) {
        List<TestPlanCase> cases = testPlanCaseRepository.findByIdIn(request.getPlanCaseIds());
        Set<Long> planIds = new HashSet<>();
        for (TestPlanCase tpc : cases) {
            String oldStatus = tpc.getStatus();
            tpc.setStatus(request.getStatus());
            tpc.setExecutedBy(request.getExecutedBy());
            tpc.setExecutedAt(LocalDateTime.now());
            if ("BLOCKED".equals(request.getStatus())) {
                tpc.setIsBlocked(true);
            } else {
                tpc.setIsBlocked(false);
                tpc.setBlockReason(null);
            }
            if (oldStatus != null && !"NOT_RUN".equals(oldStatus) && !oldStatus.equals(request.getStatus())) {
                tpc.setRetryCount(tpc.getRetryCount() + 1);
            }
            planIds.add(tpc.getTestPlan().getId());
        }
        List<TestPlanCase> saved = testPlanCaseRepository.saveAll(cases);
        for (Long planId : planIds) {
            updatePlanStatistics(planId);
        }
        return saved.stream().map(this::toTestPlanCaseResponse).toList();
    }

    public TestPlanCaseResponse assignTestCase(Long planCaseId, AssignTestCaseRequest request) {
        TestPlanCase tpc = testPlanCaseRepository.findById(planCaseId)
                .orElseThrow(() -> new RuntimeException("测试计划用例不存在: " + planCaseId));
        tpc.setAssignedTo(request.getAssignedTo());
        return toTestPlanCaseResponse(testPlanCaseRepository.save(tpc));
    }

    public List<TestPlanCaseResponse> batchAssignTestCases(Long testPlanId, BatchAssignTestCasesRequest request) {
        List<TestPlanCase> cases = testPlanCaseRepository.findByIdIn(request.getPlanCaseIds());
        for (TestPlanCase tpc : cases) {
            tpc.setAssignedTo(request.getAssignedTo());
        }
        return testPlanCaseRepository.saveAll(cases).stream().map(this::toTestPlanCaseResponse).toList();
    }

    private TestPlanCaseResponse toTestPlanCaseResponse(TestPlanCase tpc) {
        TestCaseSummary testCaseSummary = null;
        if (tpc.getTestCase() != null) {
            TestCase tc = tpc.getTestCase();
            String moduleName = null;
            if (tc.getModule() != null) {
                moduleName = tc.getModule().getName();
            }
            testCaseSummary = TestCaseSummary.builder()
                    .id(tc.getId())
                    .caseNumber(tc.getCaseNumber())
                    .title(tc.getTitle())
                    .type(tc.getType())
                    .priority(tc.getPriority())
                    .moduleName(moduleName)
                    .build();
        }
        return TestPlanCaseResponse.builder()
                .id(tpc.getId())
                .testPlanId(tpc.getTestPlan() != null ? tpc.getTestPlan().getId() : null)
                .testCase(testCaseSummary)
                .status(tpc.getStatus())
                .priority(tpc.getPriority())
                .assignedTo(tpc.getAssignedTo())
                .actualResult(tpc.getActualResult())
                .executedBy(tpc.getExecutedBy())
                .executedAt(tpc.getExecutedAt())
                .defectId(tpc.getDefectId())
                .defectLink(tpc.getDefectLink())
                .evidence(tpc.getEvidence())
                .retryCount(tpc.getRetryCount())
                .isBlocked(tpc.getIsBlocked())
                .blockReason(tpc.getBlockReason())
                .comment(tpc.getComment())
                .createdAt(tpc.getCreatedAt())
                .updatedAt(tpc.getUpdatedAt())
                .createdBy(tpc.getCreatedBy())
                .updatedBy(tpc.getUpdatedBy())
                .build();
    }
}
