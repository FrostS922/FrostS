package com.frosts.testplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank @Size(min = 1, max = 100) String oldPassword,
        @NotBlank @Size(min = 6, max = 100) String newPassword
) {
}
