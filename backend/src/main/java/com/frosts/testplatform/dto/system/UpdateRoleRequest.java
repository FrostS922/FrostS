package com.frosts.testplatform.dto.system;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateRoleRequest(
        @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,49}$") String code,
        @NotBlank @Size(max = 50) String name,
        @Size(max = 255) String description,
        Set<Long> permissionIds
) {
}
