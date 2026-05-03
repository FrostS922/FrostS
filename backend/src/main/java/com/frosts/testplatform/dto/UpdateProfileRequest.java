package com.frosts.testplatform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 50) String realName,
        @Email @Size(max = 100) String email,
        @Size(max = 20) String phone,
        @Size(max = 200) String avatar,
        @Size(max = 100) String department,
        @Size(max = 100) String position
) {
}
