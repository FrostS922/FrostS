package com.frosts.testplatform.dto.system;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
        Long id,
        String username,
        String realName,
        String email,
        String phone,
        Boolean enabled,
        List<RoleSummaryResponse> roles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
