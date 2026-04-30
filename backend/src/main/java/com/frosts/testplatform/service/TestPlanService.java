package com.frosts.testplatform.service;

import com.frosts.testplatform.entity.TestPlan;
import com.frosts.testplatform.entity.TestPlanCase;
import com.frosts.testplatform.repository.TestPlanCaseRepository;
import com.frosts.testplatform.repository.TestPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TestPlanService {

    private final TestPlanRepository testPlanRepository;
    private final TestPlanCaseRepository testPlanCaseRepository;

    @Transactional(readOnly = true)
    public Page<TestPlan> getTestPlansByProject(Long projectId, Pageable pageable) {
        return testPlanRepository.findByProjectId(projectId, pageable);
    }

    @Transactional(readOnly = true)
    public TestPlan getTestPlanById(Long id) {
        return testPlanRepository.findById(id).orElseThrow(() -> new RuntimeException("测试计划不存在: " + id));
    }

    @Transactional(readOnly = true)
    public List<TestPlanCase> getTestPlanCases(Long testPlanId) {
        return testPlanCaseRepository.findByTestPlanId(testPlanId);
    }

    public TestPlan createTestPlan(TestPlan testPlan) {
        return testPlanRepository.save(testPlan);
    }

    public TestPlan updateTestPlan(Long id, TestPlan testPlanDetails) {
        TestPlan testPlan = testPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("测试计划不存在: " + id));

        testPlan.setName(testPlanDetails.getName());
        testPlan.setDescription(testPlanDetails.getDescription());
        testPlan.setStartDate(testPlanDetails.getStartDate());
        testPlan.setEndDate(testPlanDetails.getEndDate());
        testPlan.setStatus(testPlanDetails.getStatus());
        testPlan.setTestStrategy(testPlanDetails.getTestStrategy());
        testPlan.setScope(testPlanDetails.getScope());

        return testPlanRepository.save(testPlan);
    }

    public void deleteTestPlan(Long id) {
        TestPlan testPlan = testPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("测试计划不存在: " + id));
        testPlan.setIsDeleted(true);
        testPlanRepository.save(testPlan);
    }

    public TestPlanCase addTestCaseToPlan(Long testPlanId, TestPlanCase testPlanCase) {
        testPlanCase.setTestPlan(testPlanRepository.findById(testPlanId).orElseThrow());
        return testPlanCaseRepository.save(testPlanCase);
    }

    public TestPlanCase executeTestCase(Long testPlanCaseId, String status, String actualResult, String executedBy) {
        TestPlanCase testPlanCase = testPlanCaseRepository.findById(testPlanCaseId)
                .orElseThrow(() -> new RuntimeException("测试用例不存在: " + testPlanCaseId));

        testPlanCase.setStatus(status);
        testPlanCase.setActualResult(actualResult);
        testPlanCase.setExecutedBy(executedBy);
        testPlanCase.setExecutedAt(LocalDateTime.now());

        return testPlanCaseRepository.save(testPlanCase);
    }

    public long countByStatus(Long testPlanId, String status) {
        return testPlanCaseRepository.countByTestPlanIdAndStatus(testPlanId, status);
    }
}
