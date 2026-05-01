package com.frosts.testplatform.service;

import com.frosts.testplatform.entity.Requirement;
import com.frosts.testplatform.repository.RequirementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RequirementService {

    private final RequirementRepository requirementRepository;

    @Transactional(readOnly = true)
    public Page<Requirement> getRequirementsByProject(Long projectId, Pageable pageable) {
        return requirementRepository.findByProjectId(projectId, pageable);
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
        return requirementRepository.save(requirement);
    }

    public Requirement updateRequirement(Long id, Requirement requirementDetails) {
        Requirement requirement = requirementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("需求不存在: " + id));

        if (requirementDetails.getTitle() != null) requirement.setTitle(requirementDetails.getTitle());
        if (requirementDetails.getDescription() != null) requirement.setDescription(requirementDetails.getDescription());
        if (requirementDetails.getType() != null) requirement.setType(requirementDetails.getType());
        if (requirementDetails.getPriority() != null) requirement.setPriority(requirementDetails.getPriority());
        if (requirementDetails.getStatus() != null) requirement.setStatus(requirementDetails.getStatus());
        if (requirementDetails.getParent() != null) requirement.setParent(requirementDetails.getParent());
        if (requirementDetails.getAssignedTo() != null) requirement.setAssignedTo(requirementDetails.getAssignedTo());

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
}
