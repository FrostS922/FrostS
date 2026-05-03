package com.frosts.testplatform.service;

import com.frosts.testplatform.entity.User;
import com.frosts.testplatform.event.NotificationEvent;
import com.frosts.testplatform.repository.RefreshTokenRepository;
import com.frosts.testplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityAlertService {

    private final JavaMailSender mailSender;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${spring.mail.username:noreply@frosts.com}")
    private String fromEmail;

    @Async
    public void sendTokenAbuseAlert(String username, int refreshCount, String clientIp) {
        log.info("[SECURITY_ALERT] Sending token abuse alert for user: {}, count: {}, ip: {}", username, refreshCount, clientIp);
        sendAdminEmail(username, refreshCount, clientIp);
        sendAdminNotification(username, refreshCount, clientIp);
    }

    @Async
    public void autoBanUser(String username, int refreshCount, String clientIp) {
        log.warn("[SECURITY_BAN] Auto-banning user {} for excessive token refresh ({} times/min, IP: {})",
                username, refreshCount, clientIp);

        try {
            List<com.frosts.testplatform.entity.RefreshToken> tokens =
                    refreshTokenRepository.findByUsernameAndIsRevokedFalse(username);
            tokens.forEach(t -> t.setIsRevoked(true));
            refreshTokenRepository.saveAll(tokens);

            sendBanNotification(username, refreshCount, clientIp);
            log.info("[SECURITY_BAN] User {} has been temporarily banned from token refresh", username);
        } catch (Exception e) {
            log.error("[SECURITY_BAN] Failed to auto-ban user {}: {}", username, e.getMessage());
        }
    }

    private void sendAdminEmail(String username, int refreshCount, String clientIp) {
        try {
            List<User> admins = userRepository.findByRoleCodeAndEnabled("ADMIN");
            if (admins.isEmpty()) {
                log.warn("[SECURITY_ALERT] No admin users found for alert notification");
                return;
            }

            String[] adminEmails = admins.stream()
                    .map(User::getEmail)
                    .filter(email -> email != null && !email.isEmpty())
                    .toArray(String[]::new);

            if (adminEmails.length == 0) {
                log.warn("[SECURITY_ALERT] No admin email addresses found");
                return;
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(adminEmails);
            message.setSubject("[FrostS 安全告警] Token异常刷新检测 - 用户 " + username);
            message.setText(buildAlertEmailContent(username, refreshCount, clientIp));
            mailSender.send(message);

            log.info("[SECURITY_ALERT] Alert email sent to {} admins", adminEmails.length);
        } catch (Exception e) {
            log.warn("[SECURITY_ALERT] Failed to send alert email: {}", e.getMessage());
        }
    }

    private String buildAlertEmailContent(String username, int refreshCount, String clientIp) {
        return """
                FrostS 测试平台 - 安全告警通知
                ================================
                
                告警类型：Token异常刷新
                告警时间：%s
                
                用户信息：
                  - 用户名：%s
                  - 刷新次数：%d 次/分钟
                  - 客户端IP：%s
                
                建议操作：
                  1. 检查该用户账号是否被盗用
                  2. 确认客户端IP是否为已知地址
                  3. 如确认异常，可在系统管理中禁用该用户
                  4. 如超过20次/分钟，系统将自动撤销该用户所有刷新令牌
                
                此邮件由系统自动发送，请勿回复。
                """.formatted(
                java.time.LocalDateTime.now().toString(),
                username,
                refreshCount,
                clientIp
        );
    }

    private void sendAdminNotification(String username, int refreshCount, String clientIp) {
        try {
            List<User> admins = userRepository.findByRoleCodeAndEnabled("ADMIN");
            List<Long> adminIds = admins.stream()
                    .map(User::getId)
                    .collect(Collectors.toList());

            if (adminIds.isEmpty()) {
                return;
            }

            NotificationEvent event = NotificationEvent.builder()
                    .source(this)
                    .type("SECURITY_ALERT")
                    .category("SECURITY")
                    .title("Token异常刷新告警 - 用户 " + username)
                    .content(String.format("用户 %s 在1分钟内刷新Token %d 次，客户端IP: %s，可能存在Token滥用行为。",
                            username, refreshCount, clientIp))
                    .priority("URGENT")
                    .recipientIds(adminIds)
                    .targetType("USER")
                    .targetUrl("/system")
                    .build();

            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.warn("[SECURITY_ALERT] Failed to send admin notification: {}", e.getMessage());
        }
    }

    private void sendBanNotification(String username, int refreshCount, String clientIp) {
        try {
            List<User> admins = userRepository.findByRoleCodeAndEnabled("ADMIN");
            List<Long> adminIds = admins.stream()
                    .map(User::getId)
                    .collect(Collectors.toList());

            if (adminIds.isEmpty()) {
                return;
            }

            NotificationEvent event = NotificationEvent.builder()
                    .source(this)
                    .type("SECURITY_BAN")
                    .category("SECURITY")
                    .title("用户Token刷新已被自动锁定 - " + username)
                    .content(String.format("用户 %s 在1分钟内刷新Token %d 次（超过20次阈值），客户端IP: %s，系统已自动撤销其所有刷新令牌。",
                            username, refreshCount, clientIp))
                    .priority("URGENT")
                    .recipientIds(adminIds)
                    .targetType("USER")
                    .targetUrl("/system")
                    .build();

            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.warn("[SECURITY_BAN] Failed to send ban notification: {}", e.getMessage());
        }
    }
}
