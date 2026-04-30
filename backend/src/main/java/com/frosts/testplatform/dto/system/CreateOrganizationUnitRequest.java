package com.frosts.testplatform.dto.system;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOrganizationUnitRequest(
        Long parentId,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,49}$") String code,
        @NotBlank @Size(max = 30) String type,
        @Size(max = 50) String leader,
        @Email @Size(max = 100) String contactEmail,
        @Size(max = 20) String contactPhone,
        Integer sortOrder,
        Boolean enabled,
        @Size(max = 255) String description
) {
}
