package com.frosts.testplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {

    private String token;
    private String refreshToken;
    private String type;
    private String username;
    private String realName;
    private String email;
    private java.util.List<String> roles;
    private Boolean mustChangePassword;
    private Boolean requireMfa;
    private String mfaToken;
}
