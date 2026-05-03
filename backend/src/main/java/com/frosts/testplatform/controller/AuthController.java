package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.common.Auditable;
import com.frosts.testplatform.dto.CaptchaResponse;
import com.frosts.testplatform.dto.ChangePasswordRequest;
import com.frosts.testplatform.dto.JwtResponse;
import com.frosts.testplatform.dto.LoginRequest;
import com.frosts.testplatform.dto.MfaSetupResponse;
import com.frosts.testplatform.dto.MfaVerifyRequest;
import com.frosts.testplatform.dto.ProfileResponse;
import com.frosts.testplatform.dto.RegisterRequest;
import com.frosts.testplatform.dto.RefreshTokenRequest;
import com.frosts.testplatform.entity.User;
import com.frosts.testplatform.repository.SystemSettingRepository;
import com.frosts.testplatform.repository.UserRepository;
import com.frosts.testplatform.service.AuthService;
import com.frosts.testplatform.service.CaptchaService;
import com.frosts.testplatform.service.MfaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SystemSettingRepository systemSettingRepository;
    private final CaptchaService captchaService;
    private final UserRepository userRepository;
    private final MfaService mfaService;

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        JwtResponse response = authService.login(request, clientIp, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<JwtResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (!captchaService.validateCaptcha(request.captchaKey(), request.captchaCode())) {
            throw new RuntimeException("验证码不正确或已过期");
        }
        JwtResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refreshToken(@RequestBody RefreshTokenRequest request,
                                                     HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        JwtResponse response = authService.refreshToken(request.refreshToken(), clientIp);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> getCurrentUser(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("用户不存在: " + authentication.getName()));

        ProfileResponse profile = ProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .department(user.getDepartment())
                .position(user.getPosition())
                .enabled(user.getEnabled())
                .accountNonLocked(user.getAccountNonLocked())
                .lastLoginAt(user.getLastLoginAt())
                .lastLoginIp(user.getLastLoginIp())
                .loginCount(user.getLoginCount())
                .passwordChangedAt(user.getPasswordChangedAt())
                .roles(user.getRoles().stream()
                        .map(role -> "ROLE_" + role.getCode())
                        .collect(Collectors.toList()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();

        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PostMapping("/change-password")
    @Auditable(action = "CHANGE_PASSWORD", target = "USER", description = "修改密码")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        authService.changePassword(authentication.getName(), request.oldPassword(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/registration-status")
    public ResponseEntity<ApiResponse<Boolean>> getRegistrationStatus() {
        boolean open = systemSettingRepository
                .findBySettingKeyAndIsDeletedFalse("security.open_registration")
                .map(setting -> "true".equalsIgnoreCase(setting.getSettingValue()))
                .orElse(false);
        return ResponseEntity.ok(ApiResponse.success(open));
    }

    @GetMapping("/password-policy")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPasswordPolicy() {
        int minLength = systemSettingRepository
                .findBySettingKeyAndIsDeletedFalse("security.password_min_length")
                .map(setting -> {
                    try {
                        return Integer.parseInt(setting.getSettingValue());
                    } catch (NumberFormatException e) {
                        return 6;
                    }
                })
                .orElse(6);

        String complexity = systemSettingRepository
                .findBySettingKeyAndIsDeletedFalse("security.password_complexity")
                .map(setting -> setting.getSettingValue() != null ? setting.getSettingValue() : "LOW")
                .orElse("LOW");

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "minLength", minLength,
                "complexity", complexity
        )));
    }

    @GetMapping("/captcha")
    public ResponseEntity<ApiResponse<CaptchaResponse>> getCaptcha() {
        return ResponseEntity.ok(ApiResponse.success(captchaService.generateCaptcha()));
    }

    @PostMapping("/mfa/setup")
    public ResponseEntity<ApiResponse<MfaSetupResponse>> setupMfa(Authentication authentication) {
        MfaSetupResponse response = mfaService.setupMfa(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/mfa/verify-setup")
    public ResponseEntity<ApiResponse<Void>> verifySetup(
            Authentication authentication, @RequestBody MfaVerifyRequest request) {
        mfaService.verifySetup(authentication.getName(), request.getCode());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<JwtResponse> verifyMfa(@RequestBody MfaVerifyRequest request) {
        if (!mfaService.validateMfaToken(request.getMfaToken())) {
            throw new RuntimeException("MFA令牌无效或已过期");
        }
        String username = mfaService.getUsernameFromMfaToken(request.getMfaToken());
        if (!mfaService.verifyCode(username, request.getCode())) {
            throw new RuntimeException("验证码不正确");
        }
        JwtResponse response = authService.completeMfaLogin(username);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/mfa")
    public ResponseEntity<ApiResponse<Void>> disableMfa(
            Authentication authentication, @RequestBody Map<String, String> body) {
        String password = body.get("password");
        mfaService.disableMfa(authentication.getName(), password);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/mfa/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMfaStatus(Authentication authentication) {
        boolean enabled = mfaService.isMfaEnabled(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(Map.of("enabled", enabled)));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
