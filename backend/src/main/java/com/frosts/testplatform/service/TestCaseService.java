package com.frosts.testplatform.service;

import com.frosts.testplatform.dto.testcase.TestCaseRequest;
import com.frosts.testplatform.dto.testcase.TestCaseResponse;
import com.frosts.testplatform.entity.Project;
import com.frosts.testplatform.entity.Requirement;
import com.frosts.testplatform.entity.TestCase;
import com.frosts.testplatform.entity.TestCaseModule;
import com.frosts.testplatform.mapper.TestCaseMapper;
import com.frosts.testplatform.repository.ProjectRepository;
import com.frosts.testplatform.repository.RequirementRepository;
import com.frosts.testplatform.repository.TestCaseModuleRepository;
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
    private final ProjectRepository projectRepository;
    private final RequirementRepository requirementRepository;
    private final TestCaseModuleRepository testCaseModuleRepository;
    private final TestCaseMapper testCaseMapper;

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

    public TestCaseResponse createTestCase(TestCaseRequest request) {
        TestCase testCase = new TestCase();
        testCase.setTitle(request.getTitle());
        testCase.setDescription(request.getDescription());
        testCase.setPreconditions(request.getPreconditions());
        testCase.setSteps(request.getSteps());
        testCase.setTestData(request.getTestData());
        testCase.setExpectedResults(request.getExpectedResults());
        testCase.setActualResults(request.getActualResults());
        testCase.setPostconditions(request.getPostconditions());
        testCase.setType(request.getType());
        testCase.setPriority(request.getPriority());
        testCase.setStatus(request.getStatus());
        testCase.setExecutionTime(request.getExecutionTime());
        testCase.setIsAutomated(request.getIsAutomated());
        testCase.setAutomationScript(request.getAutomationScript());
        testCase.setReviewer(request.getReviewer());
        testCase.setReviewStatus(request.getReviewStatus());
        testCase.setReviewComments(request.getReviewComments());
        testCase.setTags(request.getTags());
        testCase.setVersion(request.getVersion());

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new RuntimeException("项目不存在: " + request.getProjectId()));
        testCase.setProject(project);

        if (request.getRequirementId() != null) {
            Requirement req = requirementRepository.findById(request.getRequirementId())
                    .orElseThrow(() -> new RuntimeException("需求不存在: " + request.getRequirementId()));
            testCase.setRequirement(req);
        }
        if (request.getModuleId() != null) {
            TestCaseModule module = testCaseModuleRepository.findById(request.getModuleId())
                    .orElseThrow(() -> new RuntimeException("模块不存在: " + request.getModuleId()));
            testCase.setModule(module);
        }

        String caseNumber = generateCaseNumber(project.getId());
        testCase.setCaseNumber(caseNumber);
        if (testCase.getStatus() == null) testCase.setStatus("DRAFT");
        if (testCase.getReviewStatus() == null) testCase.setReviewStatus("PENDING");
        if (testCase.getVersion() == null) testCase.setVersion("1.0");
        if (testCase.getIsAutomated() == null) testCase.setIsAutomated(false);
        if (testCase.getPassCount() == null) testCase.setPassCount(0);
        if (testCase.getFailCount() == null) testCase.setFailCount(0);
        if (testCase.getTotalExecutions() == null) testCase.setTotalExecutions(0);
        return testCaseMapper.toResponse(testCaseRepository.save(testCase));
    }

    @Transactional(readOnly = true)
    public Page<TestCaseResponse> getTestCaseResponsesByProject(Long projectId, Pageable pageable) {
        return testCaseRepository.findByProjectId(projectId, pageable).map(testCaseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public TestCaseResponse getTestCaseResponseById(Long id) {
        TestCase tc = getTestCaseById(id);
        return testCaseMapper.toResponse(tc);
    }

    @Transactional(readOnly = true)
    public List<TestCaseResponse> getTestCaseResponsesByRequirement(Long projectId, Long requirementId) {
        return testCaseRepository.findByProjectIdAndRequirementId(projectId, requirementId)
                .stream().map(testCaseMapper::toResponse).toList();
    }

    public TestCaseResponse updateTestCase(Long id, TestCaseRequest request) {
        TestCase testCase = testCaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("测试用例不存在: " + id));

        testCase.setTitle(request.getTitle());
        testCase.setDescription(request.getDescription());
        testCase.setPreconditions(request.getPreconditions());
        testCase.setSteps(request.getSteps());
        testCase.setTestData(request.getTestData());
        testCase.setExpectedResults(request.getExpectedResults());
        testCase.setActualResults(request.getActualResults());
        testCase.setPostconditions(request.getPostconditions());
        testCase.setType(request.getType());
        testCase.setPriority(request.getPriority());
        testCase.setStatus(request.getStatus());
        testCase.setExecutionTime(request.getExecutionTime());
        testCase.setIsAutomated(request.getIsAutomated());
        testCase.setAutomationScript(request.getAutomationScript());
        testCase.setReviewer(request.getReviewer());
        testCase.setReviewStatus(request.getReviewStatus());
        testCase.setReviewComments(request.getReviewComments());
        testCase.setTags(request.getTags());
        testCase.setVersion(request.getVersion());

        if (request.getRequirementId() != null) {
            Requirement req = requirementRepository.findById(request.getRequirementId())
                    .orElseThrow(() -> new RuntimeException("需求不存在: " + request.getRequirementId()));
            testCase.setRequirement(req);
        }
        if (request.getModuleId() != null) {
            TestCaseModule module = testCaseModuleRepository.findById(request.getModuleId())
                    .orElseThrow(() -> new RuntimeException("模块不存在: " + request.getModuleId()));
            testCase.setModule(module);
        }

        return testCaseMapper.toResponse(testCaseRepository.save(testCase));
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
