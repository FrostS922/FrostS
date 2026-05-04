package com.frosts.testplatform.mapper;

import com.frosts.testplatform.dto.auditlog.AuditLogResponse;
import com.frosts.testplatform.entity.AuditLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    AuditLogResponse toResponse(AuditLog auditLog);
}
