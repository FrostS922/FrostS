package com.frosts.testplatform.dto.system;

import java.time.LocalDateTime;
import java.util.List;

public record RoleResponse(
        Long id,
        String code,
        String name,
        String description,
        Integer sortOrder,
        List<PermissionResponse> permissions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
