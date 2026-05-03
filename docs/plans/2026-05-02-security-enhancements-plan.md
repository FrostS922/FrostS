# Security Enhancements Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add three security features to FrostS: session management, MFA/TOTP, and security weekly report email.

**Architecture:** Extend existing RefreshToken entity for session tracking, add TOTP library for MFA with modified login flow, use Thymeleaf for HTML email templates with @Scheduled weekly report generation.

**Tech Stack:** Spring Boot 3.2, Spring Data JPA, Spring Security, Redis, PostgreSQL, Thymeleaf, dev.samstevens.totp, React + TypeScript + Ant Design (frontend)

---

## Phase 1: Database Migration

### Task 1: Create Flyway migration script

**Files:**
- Create: `backend/src/main/resources/db/migration/V6__add_security_enhancements.sql`

**Step 1: Write the migration SQL**

```sql
-- User table: add MFA fields
ALTER TABLE sys_user ADD COLUMN mfa_enabled BOOLEAN DEFAULT FALSE;
ALTER TABLE sys_user ADD COLUMN mfa_secret VARCHAR(256);
ALTER TABLE sys_user ADD COLUMN mfa_backup_codes VARCHAR(1000);

-- RefreshToken table: add session tracking fields
ALTER TABLE sys_refresh_token ADD COLUMN client_ip VARCHAR(50);
ALTER TABLE sys_refresh_token ADD COLUMN user_agent VARCHAR(500);
ALTER TABLE sys_refresh_token ADD COLUMN device_info VARCHAR(200);
ALTER TABLE sys_refresh_token ADD COLUMN last_refreshed_at TIMESTAMP;

-- New system settings for weekly report
INSERT INTO sys_system_setting (setting_key, setting_value, default_value, name, category, value_type, description, sort_order, is_deleted, created_at, updated_at)
VALUES
('security.weekly_report.enabled', 'true', 'true', '启用安全周报', 'NOTIFICATION', 'BOOLEAN', '是否每周自动发送安全周报邮件', 0, false, NOW(), NOW()),
('security.weekly_report.send_day', '1', '1', '周报发送日', 'NOTIFICATION', 'NUMBER', '周报发送日(1=周一,7=周日)', 1, false, NOW(), NOW()),
('security.weekly_report.send_hour', '9', '9', '周报发送时间', 'NOTIFICATION', 'NUMBER', '周报发送时间(0-23时)', 2, false, NOW(), NOW());
```

**Step 2: Restart backend to apply migration**

Run: `cd backend && mvn spring-boot:run`
Expected: Application starts, migration applied successfully, no errors in logs.

**Step 3: Commit**

```bash
git add backend/src/main/resources/db/migration/V6__add_security_enhancements.sql
git commit -m "feat: add database migration for security enhancements (MFA, sessions, weekly report)"
```

---

## Phase 2: Session Management

### Task 2: Update RefreshToken entity

**Files:**
- Modify: `backend/src/main/java/com/frosts/testplatform/entity/RefreshToken.java`

**Step 1: Add new fields to RefreshToken**

Add after existing fields:

```java
@Column(name = "client_ip", length = 50)
private String clientIp;

@Column(name = "user_agent", length = 500)
private String userAgent;

@Column(name = "device_info", length = 200)
private String deviceInfo;

@Column(name = "last_refreshed_at")
private LocalDateTime lastRefreshedAt;
```

**Step 2: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/entity/RefreshToken.java
git commit -m "feat: add session tracking fields to RefreshToken entity"
```

### Task 3: Create SessionInfo DTO

**Files:**
- Create: `backend/src/main/java/com/frosts/testplatform/dto/SessionInfo.java`

**Step 1: Write the DTO**

```java
package com.frosts.testplatform.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class SessionInfo {
    private Long id;
    private String deviceInfo;
    private String clientIp;
    private LocalDateTime createdAt;
    private LocalDateTime lastRefreshedAt;
    private Boolean current;
}
```

**Step 2: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/dto/SessionInfo.java
git commit -m "feat: add SessionInfo DTO"
```

### Task 4: Create SessionService

**Files:**
- Create: `backend/src/main/java/com/frosts/testplatform/service/SessionService.java`

**Step 1: Write the service**

```java
package com.frosts.testplatform.service;

import com.frosts.testplatform.dto.SessionInfo;
import com.frosts.testplatform.entity.RefreshToken;
import com.frosts.testplatform.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final RefreshTokenRepository refreshTokenRepository;

    public List<SessionInfo> getUserSessions(String username, Long currentTokenId) {
        List<RefreshToken> tokens = refreshTokenRepository
                .findByUsernameAndIsRevokedFalseAndExpiryDateAfter(username, LocalDateTime.now());

        return tokens.stream().map(token -> SessionInfo.builder()
                .id(token.getId())
                .deviceInfo(token.getDeviceInfo() != null ? token.getDeviceInfo() : "未知设备")
                .clientIp(token.getClientIp() != null ? token.getClientIp() : "未知IP")
                .createdAt(token.getCreatedAt())
                .lastRefreshedAt(token.getLastRefreshedAt() != null ? token.getLastRefreshedAt() : token.getCreatedAt())
                .current(token.getId().equals(currentTokenId))
                .build()
        ).collect(Collectors.toList());
    }

    @Transactional
    public void terminateSession(String username, Long sessionId) {
        RefreshToken token = refreshTokenRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("会话不存在"));
        if (!token.getUsername().equals(username)) {
            throw new RuntimeException("无权操作此会话");
        }
        token.setIsRevoked(true);
        refreshTokenRepository.save(token);
    }

    @Transactional
    public void terminateAllOtherSessions(String username, Long currentTokenId) {
        List<RefreshToken> tokens = refreshTokenRepository
                .findByUsernameAndIsRevokedFalseAndExpiryDateAfter(username, LocalDateTime.now());
        for (RefreshToken token : tokens) {
            if (!token.getId().equals(currentTokenId)) {
                token.setIsRevoked(true);
            }
        }
        refreshTokenRepository.saveAll(tokens);
    }

    public String parseDeviceInfo(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) return "未知设备";
        String browser = parseBrowser(userAgent);
        String os = parseOS(userAgent);
        return browser + " / " + os;
    }

    private String parseBrowser(String ua) {
        if (ua.contains("Edg/")) return "Edge " + extractVersion(ua, "Edg/");
        if (ua.contains("Chrome/") && !ua.contains("Edg/")) return "Chrome " + extractVersion(ua, "Chrome/");
        if (ua.contains("Firefox/")) return "Firefox " + extractVersion(ua, "Firefox/");
        if (ua.contains("Safari/") && !ua.contains("Chrome/")) return "Safari " + extractVersion(ua, "Version/");
        return "未知浏览器";
    }

    private String parseOS(String ua) {
        if (ua.contains("Windows NT 10.0")) return "Windows 10/11";
        if (ua.contains("Windows NT")) return "Windows";
        if (ua.contains("Mac OS X")) return "macOS";
        if (ua.contains("Linux")) return "Linux";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone") || ua.contains("iPad")) return "iOS";
        return "未知系统";
    }

    private String extractVersion(String ua, String prefix) {
        Pattern pattern = Pattern.compile(prefix + "([\\d.]+)");
        Matcher matcher = pattern.matcher(ua);
        if (matcher.find()) {
            String version = matcher.group(1);
            int dotIndex = version.indexOf('.', version.indexOf('.') + 1);
            return dotIndex > 0 ? version.substring(0, dotIndex) : version;
        }
        return "";
    }
}
```

**Step 2: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/service/SessionService.java
git commit -m "feat: add SessionService for session management"
```

### Task 5: Update RefreshTokenRepository

**Files:**
- Modify: `backend/src/main/java/com/frosts/testplatform/repository/RefreshTokenRepository.java`

**Step 1: Add new query method**

Add method:

```java
List<RefreshToken> findByUsernameAndIsRevokedFalseAndExpiryDateAfter(String username, LocalDateTime now);
```

**Step 2: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/repository/RefreshTokenRepository.java
git commit -m "feat: add query method for active sessions"
```

### Task 6: Update AuthService to record session info on login

**Files:**
- Modify: `backend/src/main/java/com/frosts/testplatform/service/AuthService.java`

**Step 1: In login method, set session fields on RefreshToken creation**

Find where RefreshToken is created in the login method and add:

```java
refreshToken.setClientIp(clientIp);
refreshToken.setUserAgent(userAgent);
refreshToken.setDeviceInfo(sessionService.parseDeviceInfo(userAgent));
refreshToken.setLastRefreshedAt(LocalDateTime.now());
```

Inject `SessionService` into `AuthService`.

**Step 2: In refreshToken method, update lastRefreshedAt**

```java
existingToken.setLastRefreshedAt(LocalDateTime.now());
refreshTokenRepository.save(existingToken);
```

**Step 3: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/service/AuthService.java
git commit -m "feat: record session info on login and token refresh"
```

### Task 7: Add session management API endpoints

**Files:**
- Modify: `backend/src/main/java/com/frosts/testplatform/controller/SecurityDashboardController.java`

**Step 1: Add session endpoints**

```java
@GetMapping("/sessions")
public ResponseEntity<ApiResponse<List<SessionInfo>>> getSessions(Authentication authentication) {
    String username = authentication.getName();
    List<SessionInfo> sessions = sessionService.getUserSessions(username, null);
    return ResponseEntity.ok(ApiResponse.success(sessions));
}

@DeleteMapping("/sessions/{id}")
public ResponseEntity<ApiResponse<Void>> terminateSession(
        Authentication authentication, @PathVariable Long id) {
    sessionService.terminateSession(authentication.getName(), id);
    return ResponseEntity.ok(ApiResponse.success(null));
}

@DeleteMapping("/sessions")
public ResponseEntity<ApiResponse<Void>> terminateAllOtherSessions(Authentication authentication) {
    sessionService.terminateAllOtherSessions(authentication.getName(), null);
    return ResponseEntity.ok(ApiResponse.success(null));
}
```

**Step 2: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/controller/SecurityDashboardController.java
git commit -m "feat: add session management API endpoints"
```

### Task 8: Create frontend session API

**Files:**
- Modify: `frontend/src/api/security.ts`

**Step 1: Add session API functions**

```typescript
export interface SessionInfo {
  id: number
  deviceInfo: string
  clientIp: string
  createdAt: string
  lastRefreshedAt: string
  current: boolean
}

export const getSessions = () =>
  request.get('/security/sessions') as unknown as Promise<{ code: number; data: SessionInfo[] }>

export const terminateSession = (id: number) =>
  request.delete(`/security/sessions/${id}`) as unknown as Promise<{ code: number; data: null }>

export const terminateAllOtherSessions = () =>
  request.delete('/security/sessions') as unknown as Promise<{ code: number; data: null }>
```

**Step 2: Commit**

```bash
git add frontend/src/api/security.ts
git commit -m "feat: add frontend session management API"
```

### Task 9: Create frontend SessionManagement component

**Files:**
- Create: `frontend/src/components/SessionManagement.tsx`

**Step 1: Write the component**

Use Ant Design Table, Tag, Button, Modal components. Show session list with device info, IP, last active time, current session badge, and terminate button.

**Step 2: Commit**

```bash
git add frontend/src/components/SessionManagement.tsx
git commit -m "feat: add SessionManagement frontend component"
```

### Task 10: Integrate SessionManagement into settings page

**Files:**
- Modify: `frontend/src/pages/SystemSettings.tsx` or relevant security settings page

**Step 1: Add SessionManagement tab/section**

Add a new tab "在线会话" in the security settings area that renders the SessionManagement component.

**Step 2: Commit**

```bash
git add frontend/src/pages/SystemSettings.tsx
git commit -m "feat: integrate session management into settings page"
```

---

## Phase 3: MFA/TOTP

### Task 11: Add TOTP dependency

**Files:**
- Modify: `backend/pom.xml`

**Step 1: Add dependency**

```xml
<dependency>
    <groupId>dev.samstevens.totp</groupId>
    <artifactId>totp-spring-boot-starter</artifactId>
    <version>1.7.1</version>
</dependency>
```

**Step 2: Commit**

```bash
git add backend/pom.xml
git commit -m "feat: add TOTP library dependency"
```

### Task 12: Add MFA configuration

**Files:**
- Modify: `backend/src/main/resources/application.yml`

**Step 1: Add MFA config**

```yaml
app:
  mfa:
    issuer: FrostS
    encryption-key: ${MFA_ENCRYPTION_KEY:defaultMfaEncryptionKey2024}
```

**Step 2: Commit**

```bash
git add backend/src/main/resources/application.yml
git commit -m "feat: add MFA configuration"
```

### Task 13: Update User entity with MFA fields

**Files:**
- Modify: `backend/src/main/java/com/frosts/testplatform/entity/User.java`

**Step 1: Add MFA fields**

```java
@Column(name = "mfa_enabled")
private Boolean mfaEnabled = false;

@Column(name = "mfa_secret", length = 256)
private String mfaSecret;

@Column(name = "mfa_backup_codes", length = 1000)
private String mfaBackupCodes;
```

**Step 2: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/entity/User.java
git commit -m "feat: add MFA fields to User entity"
```

### Task 14: Create MFA DTOs

**Files:**
- Create: `backend/src/main/java/com/frosts/testplatform/dto/MfaSetupResponse.java`
- Create: `backend/src/main/java/com/frosts/testplatform/dto/MfaVerifyRequest.java`

**Step 1: Write MfaSetupResponse**

```java
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
```

**Step 2: Write MfaVerifyRequest**

```java
package com.frosts.testplatform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MfaVerifyRequest {
    @NotBlank(message = "验证码不能为空")
    private String code;

    private String mfaToken;
}
```

**Step 3: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/dto/MfaSetupResponse.java
git add backend/src/main/java/com/frosts/testplatform/dto/MfaVerifyRequest.java
git commit -m "feat: add MFA DTOs"
```

### Task 15: Create MfaService

**Files:**
- Create: `backend/src/main/java/com/frosts/testplatform/service/MfaService.java`

**Step 1: Write the service**

Key methods:
- `setupMfa(username)` — Generate TOTP secret, backup codes, QR code
- `verifySetup(username, code)` — Confirm MFA binding with first TOTP code
- `verifyCode(username, code)` — Verify TOTP code during login
- `useBackupCode(username, code)` — Verify and consume a backup code
- `disableMfa(username, password)` — Disable MFA after password confirmation
- `generateBackupCodes()` — Generate 10 random 8-char backup codes
- `encryptSecret(secret)` / `decryptSecret(encrypted)` — AES encryption for TOTP secret

Use `dev.samstevens.totp`:
- `TimeBasedOneTimePasswordGenerator` for code generation/verification
- `QrData` + `ZxingPngQrGenerator` for QR code generation
- `CodeGenerator` with 6-digit codes, 30-second time step

**Step 2: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/service/MfaService.java
git commit -m "feat: add MfaService with TOTP generation and verification"
```

### Task 16: Modify login flow to support MFA

**Files:**
- Modify: `backend/src/main/java/com/frosts/testplatform/service/AuthService.java`
- Modify: `backend/src/main/java/com/frosts/testplatform/dto/JwtResponse.java`

**Step 1: Update JwtResponse**

Add fields:
```java
private Boolean requireMfa;
private String mfaToken;
```

**Step 2: Modify AuthService.login()**

After password validation succeeds:
- If `user.getMfaEnabled() == true`, return JwtResponse with `requireMfa=true` and a short-lived `mfaToken` (5 min), do NOT return access/refresh tokens yet
- If `user.getMfaEnabled() == false`, return normal JwtResponse with tokens

**Step 3: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/service/AuthService.java
git add backend/src/main/java/com/frosts/testplatform/dto/JwtResponse.java
git commit -m "feat: modify login flow to support MFA step"
```

### Task 17: Add MFA API endpoints

**Files:**
- Modify: `backend/src/main/java/com/frosts/testplatform/controller/AuthController.java`

**Step 1: Add MFA endpoints**

```java
@PostMapping("/mfa/setup")
public ResponseEntity<ApiResponse<MfaSetupResponse>> setupMfa(Authentication authentication) { ... }

@PostMapping("/mfa/verify-setup")
public ResponseEntity<ApiResponse<Void>> verifySetup(
        Authentication authentication, @RequestBody MfaVerifyRequest request) { ... }

@PostMapping("/mfa/verify")
public ResponseEntity<JwtResponse> verifyMfa(@RequestBody MfaVerifyRequest request) { ... }

@DeleteMapping("/mfa")
public ResponseEntity<ApiResponse<Void>> disableMfa(
        Authentication authentication, @RequestBody Map<String, String> body) { ... }

@GetMapping("/mfa/status")
public ResponseEntity<ApiResponse<Map<String, Object>>> getMfaStatus(Authentication authentication) { ... }
```

**Step 2: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/controller/AuthController.java
git commit -m "feat: add MFA API endpoints"
```

### Task 18: Update SecurityConfig for MFA endpoints

**Files:**
- Modify: `backend/src/main/java/com/frosts/testplatform/config/SecurityConfig.java`

**Step 1: Add MFA endpoints to permitAll**

```java
.requestMatchers("/auth/mfa/verify").permitAll()
```

Note: `/auth/mfa/setup`, `/auth/mfa/verify-setup`, `/auth/mfa/status`, `/auth/mfa` (DELETE) require authentication.

**Step 2: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/config/SecurityConfig.java
git commit -m "feat: update security config for MFA endpoints"
```

### Task 19: Create frontend MFA API

**Files:**
- Modify: `frontend/src/api/auth.ts`

**Step 1: Add MFA API functions**

```typescript
export const setupMfa = () =>
  request.post('/auth/mfa/setup') as unknown as Promise<{ code: number; data: MfaSetupData }>

export const verifyMfaSetup = (code: string) =>
  request.post('/auth/mfa/verify-setup', { code }) as unknown as Promise<{ code: number; data: null }>

export const verifyMfaLogin = (code: string, mfaToken: string) =>
  request.post('/auth/mfa/verify', { code, mfaToken }) as unknown as Promise<{ code: number; data: JwtData }>

export const disableMfa = (password: string) =>
  request.delete('/auth/mfa', { data: { password } }) as unknown as Promise<{ code: number; data: null }>

export const getMfaStatus = () =>
  request.get('/auth/mfa/status') as unknown as Promise<{ code: number; data: MfaStatusData }>
```

**Step 2: Commit**

```bash
git add frontend/src/api/auth.ts
git commit -m "feat: add frontend MFA API functions"
```

### Task 20: Create frontend MfaSetupPage component

**Files:**
- Create: `frontend/src/pages/MfaSetup.tsx`

**Step 1: Write the component**

Features:
- Display QR code image from base64
- Show secret key for manual entry
- Input field for TOTP verification code
- Display backup codes with download button
- Confirm button to complete setup

**Step 2: Commit**

```bash
git add frontend/src/pages/MfaSetup.tsx
git commit -m "feat: add MfaSetup frontend page"
```

### Task 21: Create frontend MfaVerifyPage component

**Files:**
- Create: `frontend/src/pages/MfaVerify.tsx`

**Step 1: Write the component**

Features:
- 6-digit code input with auto-submit
- "Use backup code" toggle
- Backup code input field
- Error messages for invalid codes

**Step 2: Commit**

```bash
git add frontend/src/pages/MfaVerify.tsx
git commit -m "feat: add MfaVerify frontend page"
```

### Task 22: Update login flow in frontend

**Files:**
- Modify: `frontend/src/pages/Login.tsx`
- Modify: `frontend/src/store/userStore.ts`
- Modify: `frontend/src/App.tsx`

**Step 1: Update Login page**

After login API returns `requireMfa: true`, navigate to `/mfa-verify` with `mfaToken`.

**Step 2: Update userStore**

Add `mfaToken` state, handle MFA-required login response.

**Step 3: Update App.tsx routes**

Add routes for `/mfa-verify` and `/mfa-setup`.

**Step 4: Commit**

```bash
git add frontend/src/pages/Login.tsx frontend/src/store/userStore.ts frontend/src/App.tsx
git commit -m "feat: update frontend login flow for MFA support"
```

### Task 23: Add admin forced MFA check

**Files:**
- Modify: `frontend/src/App.tsx` or route guard

**Step 1: Add MFA enforcement for admins**

In the route guard, after fetching current user profile:
- If user has ADMIN role and `mfaEnabled === false`, redirect to `/mfa-setup`

**Step 2: Commit**

```bash
git add frontend/src/App.tsx
git commit -m "feat: enforce MFA setup for admin users"
```

---

## Phase 4: Security Weekly Report

### Task 24: Add Thymeleaf dependency

**Files:**
- Modify: `backend/pom.xml`

**Step 1: Add dependency**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

**Step 2: Commit**

```bash
git add backend/pom.xml
git commit -m "feat: add Thymeleaf dependency for email templates"
```

### Task 25: Create Thymeleaf email template

**Files:**
- Create: `backend/src/main/resources/templates/email/security-weekly-report.html`

**Step 1: Write the HTML template**

Use Thymeleaf syntax with sections for:
- Header with FrostS branding
- Login statistics section (success/fail counts, daily trend table, top 5 active users)
- Banned IPs section (current banned list, new bans this week)
- Anomaly events section (suspicious IPs, token abuse, brute force attempts)
- Security indicators section (MFA enable rate, password compliance, active sessions)
- Footer with report generation timestamp

Use inline CSS for email client compatibility.

**Step 2: Commit**

```bash
git add backend/src/main/resources/templates/email/security-weekly-report.html
git commit -m "feat: add security weekly report email template"
```

### Task 26: Create SecurityWeeklyReportService

**Files:**
- Create: `backend/src/main/java/com/frosts/testplatform/service/SecurityWeeklyReportService.java`

**Step 1: Write the service**

Key methods:
- `generateAndSendWeeklyReport()` — Main entry point, collects data and sends email
- `collectLoginStats()` — Query login history for the past 7 days
- `collectBanStats()` — Get current banned IPs and new bans this week
- `collectAnomalyEvents()` — Query audit logs for security-related events
- `collectSecurityIndicators()` — Calculate MFA rate, password compliance, session count
- `sendReportEmail(htmlContent, recipients)` — Send HTML email via JavaMailSender

Use `SpringTemplateEngine` to render Thymeleaf template with data model.

**Step 2: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/service/SecurityWeeklyReportService.java
git commit -m "feat: add SecurityWeeklyReportService"
```

### Task 27: Add scheduled task for weekly report

**Files:**
- Modify: `backend/src/main/java/com/frosts/testplatform/schedule/NotificationCleanupTask.java`
  OR Create: `backend/src/main/java/com/frosts/testplatform/schedule/SecurityReportTask.java`

**Step 1: Add scheduled method**

```java
@Scheduled(cron = "0 0 9 ? * MON")
public void sendWeeklySecurityReport() {
    boolean enabled = systemSettingRepository
            .findBySettingKeyAndIsDeletedFalse("security.weekly_report.enabled")
            .map(s -> "true".equalsIgnoreCase(s.getSettingValue()))
            .orElse(true);
    if (enabled) {
        securityWeeklyReportService.generateAndSendWeeklyReport();
    }
}
```

**Step 2: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/schedule/SecurityReportTask.java
git commit -m "feat: add scheduled task for weekly security report"
```

### Task 28: Add manual trigger API for weekly report

**Files:**
- Modify: `backend/src/main/java/com/frosts/testplatform/controller/SecurityDashboardController.java`

**Step 1: Add endpoints**

```java
@PostMapping("/weekly-report/send")
@Auditable(action = "SEND_WEEKLY_REPORT", target = "SYSTEM", description = "手动发送安全周报")
public ResponseEntity<ApiResponse<Void>> sendWeeklyReport() {
    securityWeeklyReportService.generateAndSendWeeklyReport();
    return ResponseEntity.ok(ApiResponse.success(null));
}

@GetMapping("/weekly-report/preview")
public ResponseEntity<ApiResponse<String>> previewWeeklyReport() {
    String html = securityWeeklyReportService.generateReportHtml();
    return ResponseEntity.ok(ApiResponse.success(html));
}
```

**Step 2: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/controller/SecurityDashboardController.java
git commit -m "feat: add manual trigger and preview API for weekly report"
```

### Task 29: Add frontend weekly report controls

**Files:**
- Modify: `frontend/src/api/security.ts`
- Modify: `frontend/src/pages/SystemSettings.tsx` or security dashboard

**Step 1: Add API functions**

```typescript
export const sendWeeklyReport = () =>
  request.post('/security/weekly-report/send') as unknown as Promise<{ code: number; data: null }>

export const previewWeeklyReport = () =>
  request.get('/security/weekly-report/preview') as unknown as Promise<{ code: number; data: string }>
```

**Step 2: Add UI controls**

Add "发送安全周报" button and preview modal in security settings.

**Step 3: Commit**

```bash
git add frontend/src/api/security.ts frontend/src/pages/SystemSettings.tsx
git commit -m "feat: add frontend weekly report controls"
```

---

## Phase 5: Integration & Testing

### Task 30: Update DataInitializer for new settings

**Files:**
- Modify: `backend/src/main/java/com/frosts/testplatform/security/DataInitializer.java`

**Step 1: Add new system settings to initialization**

Ensure `security.weekly_report.enabled`, `security.weekly_report.send_day`, `security.weekly_report.send_hour` are initialized if not present.

**Step 2: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/security/DataInitializer.java
git commit -m "feat: update DataInitializer with new security settings"
```

### Task 31: End-to-end verification

**Step 1: Start backend**

Run: `cd backend && mvn spring-boot:run`
Expected: No startup errors, all migrations applied.

**Step 2: Start frontend**

Run: `cd frontend && npm run dev`
Expected: No compilation errors.

**Step 3: Test session management**

1. Login → verify session appears in session list
2. Login from another browser → verify two sessions
3. Terminate one session → verify it's removed
4. Verify terminated session can't refresh token

**Step 4: Test MFA flow**

1. Go to MFA setup → verify QR code displays
2. Scan with authenticator app → verify code works
3. Logout and login → verify MFA prompt appears
4. Enter TOTP code → verify successful login
5. Test backup code → verify it works once
6. Disable MFA → verify normal login resumes
7. Test admin forced MFA → verify redirect to setup

**Step 5: Test weekly report**

1. Call POST `/security/weekly-report/send` → verify email sent
2. Call GET `/security/weekly-report/preview` → verify HTML content
3. Verify scheduled task configuration

**Step 6: Final commit**

```bash
git add -A
git commit -m "feat: complete security enhancements (session management, MFA, weekly report)"
```
