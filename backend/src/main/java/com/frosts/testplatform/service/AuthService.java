package com.frosts.testplatform.service;

import com.frosts.testplatform.dto.JwtResponse;
import com.frosts.testplatform.dto.LoginRequest;
import com.frosts.testplatform.dto.RegisterRequest;
import com.frosts.testplatform.entity.RefreshToken;
import com.frosts.testplatform.entity.Role;
import com.frosts.testplatform.entity.SystemSetting;
import com.frosts.testplatform.entity.User;
import com.frosts.testplatform.entity.LoginHistory;
import com.frosts.testplatform.repository.LoginHistoryRepository;
import com.frosts.testplatform.repository.RefreshTokenRepository;
import com.frosts.testplatform.repository.RoleRepository;
import com.frosts.testplatform.repository.SystemSettingRepository;
import com.frosts.testplatform.repository.UserRepository;
import com.frosts.testplatform.security.CustomUserDetailsService;
import com.frosts.testplatform.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final LoginAnomalyAlertService loginAnomalyAlertService;
    private final PasswordEncoder passwordEncoder;
    private final TokenRefreshMonitor tokenRefreshMonitor;
    private final SessionService sessionService;
    private final MfaService mfaService;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Transactional
    public JwtResponse login(LoginRequest request, String clientIp, String userAgent) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElse(null);

        if (user != null && Boolean.FALSE.equals(user.getAccountNonLocked())) {
            recordLoginHistory(request.getUsername(), clientIp, userAgent, false, "账号已锁定");
            throw new LockedException("账号已被锁定: " + (user.getLockReason() != null ? user.getLockReason() : "登录失败次数过多"));
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            if (user != null) {
                handleLoginFailure(user);
            }
            recordLoginHistory(request.getUsername(), clientIp, userAgent, false, "用户名或密码错误");
            loginAnomalyAlertService.recordLoginFailure(clientIp);
            throw e;
        } catch (LockedException e) {
            recordLoginHistory(request.getUsername(), clientIp, userAgent, false, e.getMessage());
            loginAnomalyAlertService.recordLoginFailure(clientIp);
            throw e;
        }

        if (user != null) {
            user.setLoginFailCount(0);
            user.setAccountNonLocked(true);
            user.setLockReason(null);
            user.setLastLoginAt(LocalDateTime.now());
            user.setLastLoginIp(clientIp);
            user.setLoginCount((user.getLoginCount() != null ? user.getLoginCount() : 0) + 1);
            userRepository.save(user);
        }

        recordLoginHistory(request.getUsername(), clientIp, userAgent, true, null);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateToken(authentication);
        String refreshToken = createRefreshToken(user != null ? user.getUsername() : request.getUsername(), clientIp, userAgent);

        CustomUserDetailsService.CustomUserDetails userDetails =
                (CustomUserDetailsService.CustomUserDetails) authentication.getPrincipal();

        User freshUser = userDetails.getUser();
        boolean mustChange = Boolean.TRUE.equals(freshUser.getMustChangePassword()) || isPasswordExpired(freshUser);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toList());

        if (Boolean.TRUE.equals(freshUser.getMfaEnabled())) {
            String mfaToken = mfaService.generateMfaToken(freshUser.getUsername());
            return JwtResponse.builder()
                    .requireMfa(true)
                    .mfaToken(mfaToken)
                    .username(userDetails.getUsername())
                    .roles(roles)
                    .build();
        }

        return JwtResponse.builder()
                .token(jwt)
                .refreshToken(refreshToken)
                .type("Bearer")
                .username(userDetails.getUsername())
                .realName(freshUser.getRealName())
                .email(freshUser.getEmail())
                .roles(roles)
                .mustChangePassword(mustChange)
                .build();
    }

    @Transactional
    public JwtResponse refreshToken(String refreshTokenStr, String clientIp) {
        if (!jwtTokenProvider.validateRefreshToken(refreshTokenStr)) {
            throw new RuntimeException("无效的刷新令牌");
        }

        RefreshToken storedToken = refreshTokenRepository
                .findByTokenAndIsRevokedFalse(refreshTokenStr)
                .orElseThrow(() -> new RuntimeException("刷新令牌不存在或已撤销"));

        if (storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            storedToken.setIsRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new RuntimeException("刷新令牌已过期");
        }

        String username = storedToken.getUsername();

        if (tokenRefreshMonitor.isUserBanned(username)) {
            log.warn("[TOKEN_REFRESH_BLOCKED] 用户 {} 因频繁刷新已被临时封禁, 客户端IP: {}", username, clientIp);
            throw new RuntimeException("刷新请求过于频繁，请稍后再试");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username));

        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new RuntimeException("用户已被禁用");
        }

        tokenRefreshMonitor.recordRefresh(username, clientIp);

        storedToken.setIsRevoked(true);
        storedToken.setLastRefreshedAt(LocalDateTime.now());
        refreshTokenRepository.save(storedToken);

        String newRefreshToken = createRefreshToken(username, clientIp, null);

        CustomUserDetailsService.CustomUserDetails userDetails =
                new CustomUserDetailsService.CustomUserDetails(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        String newAccessToken = jwtTokenProvider.generateToken(authentication);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toList());

        boolean mustChange = Boolean.TRUE.equals(user.getMustChangePassword()) || isPasswordExpired(user);

        return JwtResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .type("Bearer")
                .username(userDetails.getUsername())
                .realName(user.getRealName())
                .email(user.getEmail())
                .roles(roles)
                .mustChangePassword(mustChange)
                .build();
    }

    @Transactional
    public void revokeAllRefreshTokens(String username) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUsernameAndIsRevokedFalse(username);
        tokens.forEach(t -> t.setIsRevoked(true));
        refreshTokenRepository.saveAll(tokens);
    }

    @Transactional
    public JwtResponse completeMfaLogin(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username));

        CustomUserDetailsService.CustomUserDetails userDetails =
                new CustomUserDetailsService.CustomUserDetails(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        String jwt = jwtTokenProvider.generateToken(authentication);
        String refreshToken = createRefreshToken(username, null, null);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toList());

        boolean mustChange = Boolean.TRUE.equals(user.getMustChangePassword()) || isPasswordExpired(user);

        return JwtResponse.builder()
                .token(jwt)
                .refreshToken(refreshToken)
                .type("Bearer")
                .username(userDetails.getUsername())
                .realName(user.getRealName())
                .email(user.getEmail())
                .roles(roles)
                .mustChangePassword(mustChange)
                .build();
    }

    private String createRefreshToken(String username, String clientIp, String userAgent) {
        String token = jwtTokenProvider.generateRefreshToken(username);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setUsername(username);
        refreshToken.setExpiryDate(LocalDateTime.now().plusSeconds(refreshExpiration / 1000));
        refreshToken.setIsRevoked(false);
        refreshToken.setClientIp(clientIp);
        refreshToken.setUserAgent(userAgent != null && userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent);
        refreshToken.setDeviceInfo(sessionService.parseDeviceInfo(userAgent));
        refreshToken.setLastRefreshedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);
        return token;
    }

    private boolean isPasswordExpired(User user) {
        int expireDays = getSettingAsInt("security.password_expire_days", 0);
        if (expireDays <= 0) {
            return false;
        }
        LocalDateTime changedAt = user.getPasswordChangedAt();
        if (changedAt == null) {
            return true;
        }
        return changedAt.plusDays(expireDays).isBefore(LocalDateTime.now());
    }

    private void handleLoginFailure(User user) {
        int failCount = (user.getLoginFailCount() != null ? user.getLoginFailCount() : 0) + 1;
        user.setLoginFailCount(failCount);

        int maxAttempts = getSettingAsInt("security.login_max_attempts", 5);
        if (failCount >= maxAttempts) {
            user.setAccountNonLocked(false);
            user.setLockReason("登录失败次数超过限制 (" + maxAttempts + " 次)");
        }
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username));

        if (oldPassword != null && !oldPassword.isEmpty()) {
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                throw new RuntimeException("旧密码不正确");
            }
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new RuntimeException("新密码不能与旧密码相同");
        }

        validatePassword(newPassword);

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        revokeAllRefreshTokens(username);
    }

    private void validatePassword(String password) {
        int minLength = getSettingAsInt("security.password_min_length", 6);
        if (password.length() < minLength) {
            throw new RuntimeException("密码长度不能少于 " + minLength + " 位");
        }

        String complexity = getSettingAsString("security.password_complexity", "LOW");
        switch (complexity.toUpperCase()) {
            case "MEDIUM" -> {
                if (!password.matches(".*[a-zA-Z].*") || !password.matches(".*\\d.*")) {
                    throw new RuntimeException("密码需包含字母和数字");
                }
            }
            case "HIGH" -> {
                if (!password.matches(".*[a-z].*")) {
                    throw new RuntimeException("密码需包含小写字母");
                }
                if (!password.matches(".*[A-Z].*")) {
                    throw new RuntimeException("密码需包含大写字母");
                }
                if (!password.matches(".*\\d.*")) {
                    throw new RuntimeException("密码需包含数字");
                }
                if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
                    throw new RuntimeException("密码需包含特殊字符");
                }
            }
        }
    }

    @Transactional
    public JwtResponse register(RegisterRequest request) {
        SystemSetting openRegistration = systemSettingRepository
                .findBySettingKeyAndIsDeletedFalse("security.open_registration")
                .orElseThrow(() -> new RuntimeException("系统设置不存在: security.open_registration"));

        if (!"true".equalsIgnoreCase(openRegistration.getSettingValue())) {
            throw new RuntimeException("系统未开放注册");
        }

        validatePassword(request.password());

        String username = request.username().trim();
        if (username.isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在: " + username);
        }

        String email = request.email() != null ? request.email().trim() : null;
        if (email != null && !email.isEmpty() && userRepository.existsByEmail(email)) {
            throw new RuntimeException("邮箱已存在: " + email);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRealName(request.realName() != null && !request.realName().isBlank() ? request.realName().trim() : null);
        user.setEmail(email != null && !email.isEmpty() ? email : null);
        user.setPhone(request.phone() != null && !request.phone().isBlank() ? request.phone().trim() : null);
        user.setEnabled(true);
        user.setMustChangePassword(false);
        user.setPasswordChangedAt(LocalDateTime.now());

        roleRepository.findByCode("TEST_ENGINEER").ifPresent(role ->
                user.setRoles(Set.of(role))
        );

        userRepository.save(user);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateToken(authentication);
        String refreshToken = createRefreshToken(username, null, null);

        CustomUserDetailsService.CustomUserDetails userDetails =
                (CustomUserDetailsService.CustomUserDetails) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toList());

        return JwtResponse.builder()
                .token(jwt)
                .refreshToken(refreshToken)
                .type("Bearer")
                .username(userDetails.getUsername())
                .realName(userDetails.getUser().getRealName())
                .email(userDetails.getUser().getEmail())
                .roles(roles)
                .mustChangePassword(false)
                .build();
    }

    private int getSettingAsInt(String settingKey, int defaultValue) {
        return systemSettingRepository.findBySettingKeyAndIsDeletedFalse(settingKey)
                .map(setting -> {
                    try {
                        return Integer.parseInt(setting.getSettingValue());
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    private String getSettingAsString(String settingKey, String defaultValue) {
        return systemSettingRepository.findBySettingKeyAndIsDeletedFalse(settingKey)
                .map(setting -> setting.getSettingValue() != null ? setting.getSettingValue() : defaultValue)
                .orElse(defaultValue);
    }

    private void recordLoginHistory(String username, String clientIp, String userAgent, boolean success, String failReason) {
        try {
            LoginHistory history = LoginHistory.builder()
                    .username(username)
                    .loginAt(LocalDateTime.now())
                    .loginIp(clientIp)
                    .userAgent(userAgent != null && userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent)
                    .success(success)
                    .failReason(failReason)
                    .build();
            loginHistoryRepository.save(history);
        } catch (Exception e) {
            log.warn("[LOGIN_HISTORY] 记录登录历史失败: {}", e.getMessage());
        }
    }
}
