package com.frosts.testplatform.dto.system;

import java.time.LocalDateTime;

public record PermissionResponse(
        Long id,
        String code,
        String name,
        String description,
        String resource,
        String action,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
