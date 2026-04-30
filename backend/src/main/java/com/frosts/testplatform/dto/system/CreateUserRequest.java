package com.frosts.testplatform.dto.system;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(min = 6, max = 100) String password,
        @Size(max = 50) String realName,
        @Email @Size(max = 100) String email,
        @Size(max = 20) String phone,
        Boolean enabled,
        Set<Long> roleIds
) {
}
