package com.frosts.testplatform.dto.auditlog;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        String action,
        String target,
        String targetId,
        String operator,
        String operatorIp,
        String oldValue,
        String newValue,
        LocalDateTime operatedAt,
        String description
) {
}
