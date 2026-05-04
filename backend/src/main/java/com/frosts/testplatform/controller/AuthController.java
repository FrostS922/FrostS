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
import com.frosts.testplatform.service.AuthService;
import com.frosts.testplatform.service.CaptchaService;
import com.frosts.testplatform.service.MfaService;
import com.frosts.testplatform.service.SystemSettingService;
import com.frosts.testplatform.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户登录、注册、MFA等认证操作")
public class AuthController {

    private final AuthService authService;
    private final SystemSettingService systemSettingService;
    private final CaptchaService captchaService;
    private final UserProfileService userProfileService;
    private final MfaService mfaService;

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public ResponseEntity<JwtResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        JwtResponse response = authService.login(request, clientIp, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public ResponseEntity<JwtResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (!captchaService.validateCaptcha(request.captchaKey(), request.captchaCode())) {
            throw new RuntimeException("验证码不正确或已过期");
        }
        JwtResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌")
    public ResponseEntity<JwtResponse> refreshToken(@RequestBody RefreshTokenRequest request,
                                                     HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        JwtResponse response = authService.refreshToken(request.refreshToken(), clientIp);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public ResponseEntity<ApiResponse<ProfileResponse>> getCurrentUser(Authentication authentication) {
        ProfileResponse profile = userProfileService.getProfile(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PostMapping("/change-password")
    @Auditable(action = "CHANGE_PASSWORD", target = "USER", description = "修改密码")
    @Operation(summary = "修改密码")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        authService.changePassword(authentication.getName(), request.oldPassword(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/registration-status")
    @Operation(summary = "获取注册开关状态")
    public ResponseEntity<ApiResponse<Boolean>> getRegistrationStatus() {
        return ResponseEntity.ok(ApiResponse.success(systemSettingService.isOpenRegistration()));
    }

    @GetMapping("/password-policy")
    @Operation(summary = "获取密码策略")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPasswordPolicy() {
        return ResponseEntity.ok(ApiResponse.success(systemSettingService.getPasswordPolicy()));
    }

    @GetMapping("/captcha")
    @Operation(summary = "获取验证码")
    public ResponseEntity<ApiResponse<CaptchaResponse>> getCaptcha() {
        return ResponseEntity.ok(ApiResponse.success(captchaService.generateCaptcha()));
    }

    @PostMapping("/mfa/setup")
    @Operation(summary = "设置MFA")
    public ResponseEntity<ApiResponse<MfaSetupResponse>> setupMfa(Authentication authentication) {
        MfaSetupResponse response = mfaService.setupMfa(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/mfa/verify-setup")
    @Operation(summary = "验证MFA设置")
    public ResponseEntity<ApiResponse<Void>> verifySetup(
            Authentication authentication, @RequestBody MfaVerifyRequest request) {
        mfaService.verifySetup(authentication.getName(), request.getCode());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/mfa/verify")
    @Operation(summary = "MFA登录验证")
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
    @Operation(summary = "禁用MFA")
    public ResponseEntity<ApiResponse<Void>> disableMfa(
            Authentication authentication, @RequestBody Map<String, String> body) {
        String password = body.get("password");
        mfaService.disableMfa(authentication.getName(), password);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/mfa/status")
    @Operation(summary = "获取MFA状态")
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
