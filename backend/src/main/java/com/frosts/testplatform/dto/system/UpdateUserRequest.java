package com.frosts.testplatform.dto.system;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateUserRequest(
        @Size(max = 50) String username,
        @Size(max = 50) String realName,
        @Email @Size(max = 100) String email,
        @Size(max = 20) String phone,
        @Size(max = 200) String avatar,
        @Size(max = 100) String department,
        @Size(max = 100) String position,
        Boolean enabled,
        Set<Long> roleIds
) {
}
