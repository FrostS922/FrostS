package com.frosts.testplatform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MfaVerifyRequest {
    @NotBlank(message = "验证码不能为空")
    private String code;
    private String mfaToken;
}
