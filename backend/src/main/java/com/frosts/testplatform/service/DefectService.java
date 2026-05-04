package com.frosts.testplatform.service;

import com.frosts.testplatform.dto.defect.DefectRequest;
import com.frosts.testplatform.dto.defect.DefectResponse;
import com.frosts.testplatform.entity.Defect;
import com.frosts.testplatform.entity.Project;
import com.frosts.testplatform.entity.TestCase;
import com.frosts.testplatform.entity.TestPlanCase;
import com.frosts.testplatform.event.NotificationEvent;
import com.frosts.testplatform.mapper.DefectMapper;
import com.frosts.testplatform.repository.DefectRepository;
import com.frosts.testplatform.repository.ProjectRepository;
import com.frosts.testplatform.repository.TestCaseRepository;
import com.frosts.testplatform.repository.TestPlanCaseRepository;
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
    private final ProjectRepository projectRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestPlanCaseRepository testPlanCaseRepository;
    private final DefectMapper defectMapper;
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

    public DefectResponse createDefect(DefectRequest request) {
        Defect defect = new Defect();
        defect.setTitle(request.getTitle());
        defect.setDescription(request.getDescription());
        defect.setStepsToReproduce(request.getStepsToReproduce());
        defect.setExpectedBehavior(request.getExpectedBehavior());
        defect.setActualBehavior(request.getActualBehavior());
        defect.setSeverity(request.getSeverity());
        defect.setPriority(request.getPriority());
        defect.setStatus(request.getStatus());
        defect.setType(request.getType());
        defect.setReportedBy(request.getReportedBy());
        defect.setAssignedTo(request.getAssignedTo());
        defect.setEnvironment(request.getEnvironment());
        defect.setFoundInVersion(request.getFoundInVersion());
        defect.setFixedInVersion(request.getFixedInVersion());
        defect.setComponent(request.getComponent());
        defect.setReproducibility(request.getReproducibility());
        defect.setImpact(request.getImpact());
        defect.setWorkaround(request.getWorkaround());
        defect.setRootCause(request.getRootCause());
        defect.setDuplicateOf(request.getDuplicateOf());
        defect.setSource(request.getSource());

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new RuntimeException("项目不存在: " + request.getProjectId()));
        defect.setProject(project);

        if (request.getTestCaseId() != null) {
            TestCase testCase = testCaseRepository.findById(request.getTestCaseId())
                    .orElseThrow(() -> new RuntimeException("测试用例不存在: " + request.getTestCaseId()));
            defect.setTestCase(testCase);
        }
        if (request.getTestPlanCaseId() != null) {
            TestPlanCase tpc = testPlanCaseRepository.findById(request.getTestPlanCaseId())
                    .orElseThrow(() -> new RuntimeException("测试计划用例不存在: " + request.getTestPlanCaseId()));
            defect.setTestPlanCase(tpc);
        }

        String defectNumber = generateDefectNumber(project.getId());
        defect.setDefectNumber(defectNumber);
        if (defect.getStatus() == null) {
            defect.setStatus("NEW");
        }
        defect.setReopenCount(0);
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
                    .targetUrl("/projects/" + project.getId() + "/defects")
                    .build());
        }

        return defectMapper.toResponse(saved);
    }

    public DefectResponse updateDefect(Long id, DefectRequest request) {
        Defect defect = defectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("缺陷不存在: " + id));

        String oldStatus = defect.getStatus();
        String newStatus = request.getStatus();

        defect.setTitle(request.getTitle());
        defect.setDescription(request.getDescription());
        defect.setStepsToReproduce(request.getStepsToReproduce());
        defect.setExpectedBehavior(request.getExpectedBehavior());
        defect.setActualBehavior(request.getActualBehavior());
        defect.setSeverity(request.getSeverity());
        defect.setPriority(request.getPriority());
        defect.setStatus(newStatus);
        defect.setType(request.getType());
        defect.setAssignedTo(request.getAssignedTo());
        defect.setEnvironment(request.getEnvironment());
        defect.setFoundInVersion(request.getFoundInVersion());
        defect.setFixedInVersion(request.getFixedInVersion());
        defect.setComponent(request.getComponent());
        defect.setReproducibility(request.getReproducibility());
        defect.setImpact(request.getImpact());
        defect.setWorkaround(request.getWorkaround());
        defect.setRootCause(request.getRootCause());
        defect.setDuplicateOf(request.getDuplicateOf());
        defect.setSource(request.getSource());

        if (request.getTestCaseId() != null) {
            TestCase testCase = testCaseRepository.findById(request.getTestCaseId())
                    .orElseThrow(() -> new RuntimeException("测试用例不存在: " + request.getTestCaseId()));
            defect.setTestCase(testCase);
        }
        if (request.getTestPlanCaseId() != null) {
            TestPlanCase tpc = testPlanCaseRepository.findById(request.getTestPlanCaseId())
                    .orElseThrow(() -> new RuntimeException("测试计划用例不存在: " + request.getTestPlanCaseId()));
            defect.setTestPlanCase(tpc);
        }

        // Auto-handle status transitions
        if (newStatus != null && !newStatus.equals(oldStatus)) {
            if ("RESOLVED".equals(newStatus) && !"RESOLVED".equals(oldStatus)) {
                defect.setResolvedBy(request.getResolvedBy());
                defect.setResolvedAt(LocalDateTime.now());
            }
            if ("CLOSED".equals(newStatus) && !"CLOSED".equals(oldStatus)) {
                defect.setClosedAt(LocalDateTime.now());
            }
            if ("VERIFIED".equals(newStatus)) {
                defect.setVerifiedBy(request.getVerifiedBy());
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

        return defectMapper.toResponse(defectRepository.save(defect));
    }

    public DefectResponse resolveDefect(Long id, String resolution, String resolvedBy) {
        Defect defect = defectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("缺陷不存在: " + id));

        defect.setStatus("RESOLVED");
        defect.setResolution(resolution);
        defect.setResolvedBy(resolvedBy);
        defect.setResolvedAt(LocalDateTime.now());

        return defectMapper.toResponse(defectRepository.save(defect));
    }

    public DefectResponse closeDefect(Long id, String closedBy) {
        Defect defect = defectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("缺陷不存在: " + id));

        defect.setStatus("CLOSED");
        defect.setClosedAt(LocalDateTime.now());

        return defectMapper.toResponse(defectRepository.save(defect));
    }

    public DefectResponse verifyDefect(Long id, String verifiedBy) {
        Defect defect = defectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("缺陷不存在: " + id));

        defect.setStatus("VERIFIED");
        defect.setVerifiedBy(verifiedBy);
        defect.setVerifiedAt(LocalDateTime.now());

        return defectMapper.toResponse(defectRepository.save(defect));
    }

    public DefectResponse reopenDefect(Long id) {
        Defect defect = defectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("缺陷不存在: " + id));

        defect.setStatus("REOPENED");
        defect.setReopenCount(defect.getReopenCount() + 1);
        defect.setResolvedAt(null);
        defect.setResolvedBy(null);
        defect.setClosedAt(null);
        defect.setVerifiedAt(null);
        defect.setVerifiedBy(null);

        return defectMapper.toResponse(defectRepository.save(defect));
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

    @Transactional(readOnly = true)
    public Page<DefectResponse> getDefectResponsesByProject(Long projectId, Pageable pageable) {
        return defectRepository.findByProjectId(projectId, pageable).map(defectMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public DefectResponse getDefectResponseById(Long id) {
        Defect defect = getDefectById(id);
        return defectMapper.toResponse(defect);
    }
}
