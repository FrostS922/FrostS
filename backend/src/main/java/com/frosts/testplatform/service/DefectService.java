package com.frosts.testplatform.service;

import com.frosts.testplatform.entity.Defect;
import com.frosts.testplatform.repository.DefectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class DefectService {

    private final DefectRepository defectRepository;

    @Transactional(readOnly = true)
    public Page<Defect> getDefectsByProject(Long projectId, Pageable pageable) {
        return defectRepository.findByProjectId(projectId, pageable);
    }

    @Transactional(readOnly = true)
    public Defect getDefectById(Long id) {
        return defectRepository.findById(id).orElseThrow(() -> new RuntimeException("缺陷不存在: " + id));
    }

    @Transactional(readOnly = true)
    public List<Defect> getDefectsByProjectAndStatus(Long projectId, String status) {
        return defectRepository.findByProjectIdAndStatus(projectId, status);
    }

    public Defect createDefect(Defect defect) {
        String defectNumber = generateDefectNumber(defect.getProject().getId());
        defect.setDefectNumber(defectNumber);
        defect.setStatus("NEW");
        return defectRepository.save(defect);
    }

    public Defect updateDefect(Long id, Defect defectDetails) {
        Defect defect = defectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("缺陷不存在: " + id));

        defect.setTitle(defectDetails.getTitle());
        defect.setDescription(defectDetails.getDescription());
        defect.setStepsToReproduce(defectDetails.getStepsToReproduce());
        defect.setExpectedBehavior(defectDetails.getExpectedBehavior());
        defect.setActualBehavior(defectDetails.getActualBehavior());
        defect.setSeverity(defectDetails.getSeverity());
        defect.setPriority(defectDetails.getPriority());
        defect.setStatus(defectDetails.getStatus());
        defect.setType(defectDetails.getType());
        defect.setAssignedTo(defectDetails.getAssignedTo());
        defect.setEnvironment(defectDetails.getEnvironment());
        defect.setTestCase(defectDetails.getTestCase());
        defect.setTestPlanCase(defectDetails.getTestPlanCase());

        return defectRepository.save(defect);
    }

    public Defect resolveDefect(Long id, String resolution, String resolvedBy) {
        Defect defect = defectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("缺陷不存在: " + id));

        defect.setStatus("RESOLVED");
        defect.setResolution(resolution);
        defect.setResolvedBy(resolvedBy);
        defect.setResolvedAt(LocalDateTime.now());

        return defectRepository.save(defect);
    }

    public Defect closeDefect(Long id, String closedBy) {
        Defect defect = defectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("缺陷不存在: " + id));

        defect.setStatus("CLOSED");
        defect.setClosedAt(LocalDateTime.now());

        return defectRepository.save(defect);
    }

    public void deleteDefect(Long id) {
        Defect defect = defectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("缺陷不存在: " + id));
        defect.setIsDeleted(true);
        defectRepository.save(defect);
    }

    public Map<String, Object> getDefectStatistics(Long projectId) {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("total", defectRepository.count());
        stats.put("new", defectRepository.countByProjectIdAndStatus(projectId, "NEW"));
        stats.put("open", defectRepository.countByProjectIdAndStatus(projectId, "OPEN"));
        stats.put("resolved", defectRepository.countByProjectIdAndStatus(projectId, "RESOLVED"));
        stats.put("closed", defectRepository.countByProjectIdAndStatus(projectId, "CLOSED"));
        return stats;
    }

    private String generateDefectNumber(Long projectId) {
        String prefix = "DEF-" + String.format("%04d", projectId) + "-";
        long count = defectRepository.count() + 1;
        return prefix + String.format("%06d", count);
    }
}
