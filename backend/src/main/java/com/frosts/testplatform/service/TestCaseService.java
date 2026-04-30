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
        return testCaseRepository.save(testCase);
    }

    public TestCase updateTestCase(Long id, TestCase testCaseDetails) {
        TestCase testCase = testCaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("测试用例不存在: " + id));

        testCase.setTitle(testCaseDetails.getTitle());
        testCase.setDescription(testCaseDetails.getDescription());
        testCase.setPreconditions(testCaseDetails.getPreconditions());
        testCase.setSteps(testCaseDetails.getSteps());
        testCase.setExpectedResults(testCaseDetails.getExpectedResults());
        testCase.setActualResults(testCaseDetails.getActualResults());
        testCase.setType(testCaseDetails.getType());
        testCase.setPriority(testCaseDetails.getPriority());
        testCase.setStatus(testCaseDetails.getStatus());
        testCase.setModule(testCaseDetails.getModule());
        testCase.setRequirement(testCaseDetails.getRequirement());

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
