package com.frosts.testplatform.service;

import com.frosts.testplatform.dto.RequirementCoverageDTO;
import com.frosts.testplatform.dto.requirement.RequirementRequest;
import com.frosts.testplatform.dto.requirement.RequirementResponse;
import com.frosts.testplatform.entity.Project;
import com.frosts.testplatform.entity.Requirement;
import com.frosts.testplatform.entity.TestCase;
import com.frosts.testplatform.mapper.RequirementMapper;
import com.frosts.testplatform.repository.ProjectRepository;
import com.frosts.testplatform.repository.RequirementRepository;
import com.frosts.testplatform.repository.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RequirementService {

    private final RequirementRepository requirementRepository;
    private final TestCaseRepository testCaseRepository;
    private final ProjectRepository projectRepository;
    private final RequirementMapper requirementMapper;

    @Transactional(readOnly = true)
    public Page<Requirement> getRequirementsByProject(Long projectId, Pageable pageable) {
        Page<Requirement> page = requirementRepository.findByProjectId(projectId, pageable);
        page.getContent().forEach(req -> {
            long count = testCaseRepository.countByRequirementId(req.getId());
            req.setTestCaseCount(count);
            req.setCoverageRate(count > 0 ? 100.0 : 0.0);
        });
        return page;
    }

    @Transactional(readOnly = true)
    public Requirement getRequirementById(Long id) {
        return requirementRepository.findById(id).orElseThrow(() -> new RuntimeException("需求不存在: " + id));
    }

    @Transactional(readOnly = true)
    public List<Requirement> getRootRequirements(Long projectId) {
        return requirementRepository.findByProjectIdAndParentIdIsNull(projectId);
    }

    public RequirementResponse createRequirement(RequirementRequest request) {
        Requirement requirement = new Requirement();
        requirement.setTitle(request.getTitle());
        requirement.setDescription(request.getDescription());
        requirement.setAcceptanceCriteria(request.getAcceptanceCriteria());
        requirement.setType(request.getType());
        requirement.setPriority(request.getPriority());
        requirement.setStatus(request.getStatus());
        if (request.getStatus() != null) {
            String oldStatus = requirement.getStatus();
            String newStatus = request.getStatus();
            if ("APPROVED".equals(newStatus) && !"APPROVED".equals(oldStatus)) {
            }
            if ("REJECTED".equals(newStatus)) {
                requirement.setRejectedReason(request.getRejectedReason());
            }
            if ("COMPLETED".equals(newStatus) && !"COMPLETED".equals(oldStatus)) {
                requirement.setCompletedDate(java.time.LocalDate.now());
            }
        }
        requirement.setAssignedTo(request.getAssignedTo());
        requirement.setStoryPoints(request.getStoryPoints());
        requirement.setEstimatedHours(request.getEstimatedHours());
        requirement.setActualHours(request.getActualHours());
        requirement.setDueDate(request.getDueDate());
        requirement.setSource(request.getSource());
        requirement.setRejectedReason(request.getRejectedReason());

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new RuntimeException("项目不存在: " + request.getProjectId()));
        requirement.setProject(project);

        if (request.getParentId() != null) {
            Requirement parent = requirementRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("父需求不存在: " + request.getParentId()));
            requirement.setParent(parent);
        }

        String reqNumber = generateRequirementNumber(project.getId());
        requirement.setRequirementNumber(reqNumber);
        if (requirement.getStatus() == null) requirement.setStatus("DRAFT");
        return requirementMapper.toResponse(requirementRepository.save(requirement));
    }

    public RequirementResponse updateRequirement(Long id, RequirementRequest request) {
        Requirement requirement = getRequirementById(id);
        requirement.setTitle(request.getTitle());
        requirement.setDescription(request.getDescription());
        requirement.setAcceptanceCriteria(request.getAcceptanceCriteria());
        requirement.setType(request.getType());
        requirement.setPriority(request.getPriority());
        requirement.setStatus(request.getStatus());
        requirement.setAssignedTo(request.getAssignedTo());
        requirement.setStoryPoints(request.getStoryPoints());
        requirement.setEstimatedHours(request.getEstimatedHours());
        requirement.setActualHours(request.getActualHours());
        requirement.setDueDate(request.getDueDate());
        requirement.setSource(request.getSource());
        requirement.setRejectedReason(request.getRejectedReason());

        if (request.getParentId() != null) {
            Requirement parent = requirementRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("父需求不存在: " + request.getParentId()));
            requirement.setParent(parent);
        } else {
            requirement.setParent(null);
        }

        return requirementMapper.toResponse(requirementRepository.save(requirement));
    }

    public void deleteRequirement(Long id) {
        Requirement requirement = requirementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("需求不存在: " + id));
        requirement.setIsDeleted(true);
        requirementRepository.save(requirement);
    }

    private String generateRequirementNumber(Long projectId) {
        String prefix = "REQ-" + String.format("%04d", projectId) + "-";
        long count = requirementRepository.count() + 1;
        return prefix + String.format("%06d", count);
    }

    @Transactional(readOnly = true)
    public RequirementCoverageDTO getRequirementCoverage(Long projectId, Long requirementId) {
        Requirement requirement = requirementRepository.findById(requirementId)
                .orElseThrow(() -> new RuntimeException("需求不存在: " + requirementId));

        List<TestCase> testCases = testCaseRepository.findByProjectIdAndRequirementId(projectId, requirementId);

        RequirementCoverageDTO dto = new RequirementCoverageDTO();
        dto.setRequirementId(requirementId);
        dto.setRequirementNumber(requirement.getRequirementNumber());
        dto.setRequirementTitle(requirement.getTitle());
        dto.setTotalTestCases(testCases.size());
        dto.setActiveTestCases((int) testCases.stream().filter(tc -> "ACTIVE".equals(tc.getStatus())).count());

        if (!testCases.isEmpty()) {
            dto.setCoverageRate(Math.round(testCases.stream()
                    .filter(tc -> "ACTIVE".equals(tc.getStatus())).count() * 100.0 / testCases.size() * 100.0) / 100.0);
        } else {
            dto.setCoverageRate(0.0);
        }

        Map<String, Long> statusDist = testCases.stream()
                .collect(Collectors.groupingBy(
                        tc -> tc.getStatus() != null ? tc.getStatus() : "UNKNOWN",
                        Collectors.counting()));
        dto.setTestCaseStatusDistribution(statusDist);

        List<RequirementCoverageDTO.TestCaseSummary> summaries = testCases.stream().map(tc -> {
            RequirementCoverageDTO.TestCaseSummary summary = new RequirementCoverageDTO.TestCaseSummary();
            summary.setId(tc.getId());
            summary.setCaseNumber(tc.getCaseNumber());
            summary.setTitle(tc.getTitle());
            summary.setType(tc.getType());
            summary.setPriority(tc.getPriority());
            summary.setStatus(tc.getStatus());
            return summary;
        }).collect(Collectors.toList());
        dto.setTestCases(summaries);

        return dto;
    }

    @Transactional(readOnly = true)
    public Page<RequirementResponse> getRequirementResponsesByProject(Long projectId, Pageable pageable) {
        Page<Requirement> page = requirementRepository.findByProjectId(projectId, pageable);
        page.getContent().forEach(req -> {
            long count = testCaseRepository.countByRequirementId(req.getId());
            req.setTestCaseCount(count);
            req.setCoverageRate(count > 0 ? 100.0 : 0.0);
        });
        return page.map(requirementMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RequirementResponse getRequirementResponseById(Long id) {
        Requirement req = getRequirementById(id);
        return requirementMapper.toResponse(req);
    }

    @Transactional(readOnly = true)
    public List<RequirementResponse> getRootRequirementResponses(Long projectId) {
        return requirementRepository.findByProjectIdAndParentIdIsNull(projectId)
                .stream().map(requirementMapper::toResponse).toList();
    }
}
