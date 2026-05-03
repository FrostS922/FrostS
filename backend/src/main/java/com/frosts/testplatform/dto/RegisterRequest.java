package com.frosts.testplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(min = 6, max = 100) String password,
        @Size(max = 50) String realName,
        @Size(max = 100) String email,
        @Size(max = 20) String phone,
        String captchaKey,
        String captchaCode
) {
}
