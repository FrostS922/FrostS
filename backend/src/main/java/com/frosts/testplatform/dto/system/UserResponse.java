package com.frosts.testplatform.dto.system;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
        Long id,
        String username,
        String realName,
        String email,
        String phone,
        String avatar,
        String department,
        String position,
        Boolean enabled,
        Boolean accountNonLocked,
        List<RoleSummaryResponse> roles,
        String generatedPassword,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
