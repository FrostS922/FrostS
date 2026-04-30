package com.frosts.testplatform.dto.system;

import java.time.LocalDateTime;
import java.util.List;

public record OrganizationUnitResponse(
        Long id,
        Long parentId,
        String code,
        String name,
        String type,
        String leader,
        String contactEmail,
        String contactPhone,
        Integer sortOrder,
        Boolean enabled,
        String description,
        List<OrganizationUnitResponse> children,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
