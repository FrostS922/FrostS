package com.frosts.testplatform.service;

import com.frosts.testplatform.entity.TestCase;
import com.frosts.testplatform.repository.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TestCaseService {

    private final TestCaseRepository testCaseRepository;

    @Transactional(readOnly = true)
    public Page<TestCase> getTestCasesByProject(Long projectId, Pageable pageable) {
        return testCaseRepository.findByProjectId(projectId, pageable);
    }

    @Transactional(readOnly = true)
    public TestCase getTestCaseById(Long id) {
        return testCaseRepository.findById(id).orElseThrow(() -> new RuntimeException("测试用例不存在: " + id));
    }

    @Transactional(readOnly = true)
    public List<TestCase> getTestCasesByProjectAndStatus(Long projectId, String status) {
        return testCaseRepository.findByProjectIdAndStatus(projectId, status);
    }

    public TestCase createTestCase(TestCase testCase) {
        String caseNumber = generateCaseNumber(testCase.getProject().getId());
        testCase.setCaseNumber(caseNumber);
        if (testCase.getStatus() == null) {
            testCase.setStatus("DRAFT");
        }
        if (testCase.getReviewStatus() == null) {
            testCase.setReviewStatus("PENDING");
        }
        if (testCase.getVersion() == null) {
            testCase.setVersion("1.0");
        }
        if (testCase.getIsAutomated() == null) {
            testCase.setIsAutomated(false);
        }
        if (testCase.getPassCount() == null) {
            testCase.setPassCount(0);
        }
        if (testCase.getFailCount() == null) {
            testCase.setFailCount(0);
        }
        if (testCase.getTotalExecutions() == null) {
            testCase.setTotalExecutions(0);
        }
        return testCaseRepository.save(testCase);
    }

    public TestCase updateTestCase(Long id, TestCase testCaseDetails) {
        TestCase testCase = testCaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("测试用例不存在: " + id));

        testCase.setTitle(testCaseDetails.getTitle());
        testCase.setDescription(testCaseDetails.getDescription());
        testCase.setPreconditions(testCaseDetails.getPreconditions());
        testCase.setSteps(testCaseDetails.getSteps());
        testCase.setTestData(testCaseDetails.getTestData());
        testCase.setExpectedResults(testCaseDetails.getExpectedResults());
        testCase.setActualResults(testCaseDetails.getActualResults());
        testCase.setPostconditions(testCaseDetails.getPostconditions());
        testCase.setType(testCaseDetails.getType());
        testCase.setPriority(testCaseDetails.getPriority());
        testCase.setStatus(testCaseDetails.getStatus());
        testCase.setExecutionTime(testCaseDetails.getExecutionTime());
        testCase.setIsAutomated(testCaseDetails.getIsAutomated());
        testCase.setAutomationScript(testCaseDetails.getAutomationScript());
        testCase.setReviewer(testCaseDetails.getReviewer());
        testCase.setReviewStatus(testCaseDetails.getReviewStatus());
        testCase.setReviewComments(testCaseDetails.getReviewComments());
        testCase.setTags(testCaseDetails.getTags());
        testCase.setVersion(testCaseDetails.getVersion());
        testCase.setModule(testCaseDetails.getModule());
        testCase.setRequirement(testCaseDetails.getRequirement());

        return testCaseRepository.save(testCase);
    }

    public TestCase recordExecution(Long id, boolean passed, String executedBy) {
        TestCase testCase = testCaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("测试用例不存在: " + id));

        testCase.setTotalExecutions(testCase.getTotalExecutions() + 1);
        if (passed) {
            testCase.setPassCount(testCase.getPassCount() + 1);
        } else {
            testCase.setFailCount(testCase.getFailCount() + 1);
        }
        testCase.setLastExecutedAt(java.time.LocalDateTime.now());
        testCase.setLastExecutedBy(executedBy);

        return testCaseRepository.save(testCase);
    }

    public void deleteTestCase(Long id) {
        TestCase testCase = testCaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("测试用例不存在: " + id));
        testCase.setIsDeleted(true);
        testCaseRepository.save(testCase);
    }

    public List<TestCase> getTestCasesByRequirement(Long projectId, Long requirementId) {
        return testCaseRepository.findByProjectIdAndRequirementId(projectId, requirementId);
    }

    private String generateCaseNumber(Long projectId) {
        String prefix = "TC-" + String.format("%04d", projectId) + "-";
        long count = testCaseRepository.count() + 1;
        return prefix + String.format("%06d", count);
    }
}
