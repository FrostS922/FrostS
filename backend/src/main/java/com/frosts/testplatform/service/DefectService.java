package com.frosts.testplatform.service;

import com.frosts.testplatform.entity.Defect;
import com.frosts.testplatform.event.NotificationEvent;
import com.frosts.testplatform.repository.DefectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

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
        if (defect.getStatus() == null) {
            defect.setStatus("NEW");
        }
        if (defect.getReopenCount() == null) {
            defect.setReopenCount(0);
        }
        Defect saved = defectRepository.save(defect);

        if (defect.getAssignedTo() != null) {
            eventPublisher.publishEvent(NotificationEvent.builder()
                    .source(this)
                    .type("BUSINESS")
                    .category("DEFECT_ASSIGNED")
                    .title("新缺陷分配: " + defect.getTitle())
                    .content("缺陷编号 " + defectNumber + " 已分配给您处理")
                    .priority("HIGH")
                    .targetType("DEFECT")
                    .targetId(saved.getId())
                    .targetUrl("/projects/" + defect.getProject().getId() + "/defects")
                    .build());
        }

        return saved;
    }

    public Defect updateDefect(Long id, Defect defectDetails) {
        Defect defect = defectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("缺陷不存在: " + id));

        String oldStatus = defect.getStatus();
        String newStatus = defectDetails.getStatus();

        defect.setTitle(defectDetails.getTitle());
        defect.setDescription(defectDetails.getDescription());
        defect.setStepsToReproduce(defectDetails.getStepsToReproduce());
        defect.setExpectedBehavior(defectDetails.getExpectedBehavior());
        defect.setActualBehavior(defectDetails.getActualBehavior());
        defect.setSeverity(defectDetails.getSeverity());
        defect.setPriority(defectDetails.getPriority());
        defect.setStatus(newStatus);
        defect.setType(defectDetails.getType());
        defect.setAssignedTo(defectDetails.getAssignedTo());
        defect.setEnvironment(defectDetails.getEnvironment());
        defect.setFoundInVersion(defectDetails.getFoundInVersion());
        defect.setFixedInVersion(defectDetails.getFixedInVersion());
        defect.setComponent(defectDetails.getComponent());
        defect.setReproducibility(defectDetails.getReproducibility());
        defect.setImpact(defectDetails.getImpact());
        defect.setWorkaround(defectDetails.getWorkaround());
        defect.setRootCause(defectDetails.getRootCause());
        defect.setDuplicateOf(defectDetails.getDuplicateOf());
        defect.setSource(defectDetails.getSource());
        defect.setTestCase(defectDetails.getTestCase());
        defect.setTestPlanCase(defectDetails.getTestPlanCase());

        // Auto-handle status transitions
        if (newStatus != null && !newStatus.equals(oldStatus)) {
            if ("RESOLVED".equals(newStatus) && !"RESOLVED".equals(oldStatus)) {
                defect.setResolvedBy(defectDetails.getResolvedBy());
                defect.setResolvedAt(LocalDateTime.now());
            }
            if ("CLOSED".equals(newStatus) && !"CLOSED".equals(oldStatus)) {
                defect.setClosedAt(LocalDateTime.now());
            }
            if ("VERIFIED".equals(newStatus)) {
                defect.setVerifiedBy(defectDetails.getVerifiedBy());
                defect.setVerifiedAt(LocalDateTime.now());
            }
            if ("REOPENED".equals(newStatus) && ("RESOLVED".equals(oldStatus) || "CLOSED".equals(oldStatus))) {
                defect.setReopenCount(defect.getReopenCount() + 1);
                defect.setResolvedAt(null);
                defect.setResolvedBy(null);
                defect.setClosedAt(null);
            }

            eventPublisher.publishEvent(NotificationEvent.builder()
                    .source(this)
                    .type("BUSINESS")
                    .category("DEFECT_STATUS_CHANGED")
                    .title("缺陷状态变更: " + defect.getTitle())
                    .content("缺陷编号 " + defect.getDefectNumber() + " 状态从 " + oldStatus + " 变更为 " + newStatus)
                    .targetType("DEFECT")
                    .targetId(id)
                    .targetUrl("/projects/" + defect.getProject().getId() + "/defects")
                    .build());
        }

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

    public Defect verifyDefect(Long id, String verifiedBy) {
        Defect defect = defectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("缺陷不存在: " + id));

        defect.setStatus("VERIFIED");
        defect.setVerifiedBy(verifiedBy);
        defect.setVerifiedAt(LocalDateTime.now());

        return defectRepository.save(defect);
    }

    public Defect reopenDefect(Long id) {
        Defect defect = defectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("缺陷不存在: " + id));

        defect.setStatus("REOPENED");
        defect.setReopenCount(defect.getReopenCount() + 1);
        defect.setResolvedAt(null);
        defect.setResolvedBy(null);
        defect.setClosedAt(null);
        defect.setVerifiedAt(null);
        defect.setVerifiedBy(null);

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
