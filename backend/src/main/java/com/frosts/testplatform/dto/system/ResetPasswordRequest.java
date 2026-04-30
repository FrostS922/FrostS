package com.frosts.testplatform.dto.system;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Size(min = 6, max = 100) String password
) {
}
