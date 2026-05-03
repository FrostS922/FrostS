package com.frosts.testplatform.service;

import com.frosts.testplatform.dto.RequirementCoverageDTO;
import com.frosts.testplatform.entity.Requirement;
import com.frosts.testplatform.entity.TestCase;
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

    public Requirement createRequirement(Requirement requirement) {
        String reqNumber = generateRequirementNumber(requirement.getProject().getId());
        requirement.setRequirementNumber(reqNumber);
        if (requirement.getStatus() == null) {
            requirement.setStatus("DRAFT");
        }
        return requirementRepository.save(requirement);
    }

    public Requirement updateRequirement(Long id, Requirement requirementDetails) {
        Requirement requirement = requirementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("需求不存在: " + id));

        if (requirementDetails.getTitle() != null) requirement.setTitle(requirementDetails.getTitle());
        if (requirementDetails.getDescription() != null) requirement.setDescription(requirementDetails.getDescription());
        if (requirementDetails.getAcceptanceCriteria() != null) requirement.setAcceptanceCriteria(requirementDetails.getAcceptanceCriteria());
        if (requirementDetails.getType() != null) requirement.setType(requirementDetails.getType());
        if (requirementDetails.getPriority() != null) requirement.setPriority(requirementDetails.getPriority());
        if (requirementDetails.getStatus() != null) {
            String oldStatus = requirement.getStatus();
            String newStatus = requirementDetails.getStatus();
            requirement.setStatus(newStatus);
            if ("APPROVED".equals(newStatus) && !"APPROVED".equals(oldStatus)) {
                // Optionally trigger approval workflow
            }
            if ("REJECTED".equals(newStatus)) {
                requirement.setRejectedReason(requirementDetails.getRejectedReason());
            }
            if ("COMPLETED".equals(newStatus) && !"COMPLETED".equals(oldStatus)) {
                requirement.setCompletedDate(java.time.LocalDate.now());
            }
        }
        if (requirementDetails.getParent() != null) requirement.setParent(requirementDetails.getParent());
        if (requirementDetails.getAssignedTo() != null) requirement.setAssignedTo(requirementDetails.getAssignedTo());
        if (requirementDetails.getStoryPoints() != null) requirement.setStoryPoints(requirementDetails.getStoryPoints());
        if (requirementDetails.getEstimatedHours() != null) requirement.setEstimatedHours(requirementDetails.getEstimatedHours());
        if (requirementDetails.getActualHours() != null) requirement.setActualHours(requirementDetails.getActualHours());
        if (requirementDetails.getDueDate() != null) requirement.setDueDate(requirementDetails.getDueDate());
        if (requirementDetails.getSource() != null) requirement.setSource(requirementDetails.getSource());

        return requirementRepository.save(requirement);
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
}
