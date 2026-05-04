package com.frosts.testplatform.service;

import com.frosts.testplatform.dto.auditlog.AuditLogResponse;
import com.frosts.testplatform.entity.AuditLog;
import com.frosts.testplatform.mapper.AuditLogMapper;
import com.frosts.testplatform.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    public Page<AuditLogResponse> getAuditLogs(String action, String operator, Pageable pageable) {
        Page<AuditLog> result;
        if (operator != null && !operator.isBlank()) {
            result = auditLogRepository.findByOperatorOrderByOperatedAtDesc(operator, pageable);
        } else if (action != null && !action.isBlank()) {
            result = auditLogRepository.findByActionOrderByOperatedAtDesc(action, pageable);
        } else {
            result = auditLogRepository.findAllByOrderByOperatedAtDesc(pageable);
        }
        return result.map(auditLogMapper::toResponse);
    }

    public List<AuditLog> getAuditLogsForExport(String action, String operator, Pageable pageable) {
        if (operator != null && !operator.isBlank()) {
            return auditLogRepository.findByOperatorOrderByOperatedAtDesc(operator, pageable).getContent();
        } else if (action != null && !action.isBlank()) {
            return auditLogRepository.findByActionOrderByOperatedAtDesc(action, pageable).getContent();
        } else {
            return auditLogRepository.findAllByOrderByOperatedAtDesc(pageable).getContent();
        }
    }
}
