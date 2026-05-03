package com.frosts.testplatform.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class MfaSetupResponse {
    private String secret;
    private String otpAuthUrl;
    private String qrCodeBase64;
    private List<String> backupCodes;
}
