package com.frosts.testplatform.service;

import com.frosts.testplatform.entity.SystemSetting;
import com.frosts.testplatform.entity.User;
import com.frosts.testplatform.event.NotificationEvent;
import com.frosts.testplatform.repository.SystemSettingRepository;
import com.frosts.testplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAnomalyAlertService {

    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;
    private final SystemSettingRepository systemSettingRepository;

    @Value("${spring.mail.username:noreply@frosts.com}")
    private String fromEmail;

    private static final String FAIL_KEY_PREFIX = "login_fail:";
    private static final String BAN_KEY_PREFIX = "ip_banned:";

    public void recordLoginFailure(String clientIp) {
        if (clientIp == null || clientIp.isEmpty()) return;

        int warnThreshold = getSettingAsInt("security.login.warn_threshold", 5);
        int alertThreshold = getSettingAsInt("security.login.alert_threshold", 10);
        int banThreshold = getSettingAsInt("security.login.ban_threshold", 20);
        int windowMinutes = getSettingAsInt("security.login.window_minutes", 5);
        int banMinutes = getSettingAsInt("security.login.ban_minutes", 30);

        String key = FAIL_KEY_PREFIX + clientIp;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, windowMinutes, TimeUnit.MINUTES);
        }

        if (count != null) {
            if (count >= banThreshold && count == banThreshold) {
                log.warn("[LOGIN_ANOMALY] IP {} 在 {} 分钟内登录失败 {} 次，触发自动封禁 {} 分钟",
                        clientIp, windowMinutes, count, banMinutes);
                banIp(clientIp, banMinutes);
                sendLoginAnomalyAlert(clientIp, count.intValue(), "URGENT", true, windowMinutes, banMinutes);
            } else if (count >= alertThreshold && count == alertThreshold) {
                log.warn("[LOGIN_ANOMALY] IP {} 在 {} 分钟内登录失败 {} 次，触发告警", clientIp, windowMinutes, count);
                sendLoginAnomalyAlert(clientIp, count.intValue(), "URGENT", false, windowMinutes, banMinutes);
            } else if (count >= warnThreshold && count == warnThreshold) {
                log.info("[LOGIN_ANOMALY] IP {} 在 {} 分钟内登录失败 {} 次，触发预警", clientIp, windowMinutes, count);
                sendLoginAnomalyAlert(clientIp, count.intValue(), "WARNING", false, windowMinutes, banMinutes);
            }
        }
    }

    public boolean isIpBanned(String clientIp) {
        if (clientIp == null || clientIp.isEmpty()) return false;
        return Boolean.TRUE.equals(redisTemplate.hasKey(BAN_KEY_PREFIX + clientIp));
    }

    public void banIp(String clientIp, int minutes) {
        redisTemplate.opsForValue().set(BAN_KEY_PREFIX + clientIp, LocalDateTime.now().toString(), minutes, TimeUnit.MINUTES);
        log.info("[IP_BAN] IP {} 已被封禁 {} 分钟", clientIp, minutes);
    }

    public void unbanIp(String clientIp) {
        redisTemplate.delete(BAN_KEY_PREFIX + clientIp);
        redisTemplate.delete(FAIL_KEY_PREFIX + clientIp);
        log.info("[IP_BAN] IP {} 已解除封禁", clientIp);
    }

    public long getBannedIpCount() {
        var keys = redisTemplate.keys(BAN_KEY_PREFIX + "*");
        return keys != null ? keys.size() : 0;
    }

    public List<Map<String, String>> getBannedIps() {
        var keys = redisTemplate.keys(BAN_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) return List.of();

        return keys.stream().map(key -> {
            String ip = key.substring(BAN_KEY_PREFIX.length());
            String bannedAt = redisTemplate.opsForValue().get(key);
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return Map.of(
                    "ip", ip,
                    "bannedAt", bannedAt != null ? bannedAt : "",
                    "remainingSeconds", String.valueOf(ttl != null ? ttl : 0)
            );
        }).collect(Collectors.toList());
    }

    @Async
    public void sendLoginAnomalyAlert(String clientIp, int failCount, String priority,
                                       boolean banned, int windowMinutes, int banMinutes) {
        try {
            sendAdminEmail(clientIp, failCount, banned, windowMinutes, banMinutes);
            sendAdminNotification(clientIp, failCount, priority, banned, windowMinutes);
        } catch (Exception e) {
            log.warn("[LOGIN_ANOMALY] 发送登录异常告警失败: {}", e.getMessage());
        }
    }

    private void sendAdminEmail(String clientIp, int failCount, boolean banned,
                                 int windowMinutes, int banMinutes) {
        try {
            List<User> admins = userRepository.findByRoleCodeAndEnabled("ADMIN");
            if (admins.isEmpty()) return;

            String[] adminEmails = admins.stream()
                    .map(User::getEmail)
                    .filter(email -> email != null && !email.isEmpty())
                    .toArray(String[]::new);

            if (adminEmails.length == 0) return;

            String statusLine = banned
                    ? "处理结果：该IP已被自动封禁 " + banMinutes + " 分钟"
                    : "建议操作：考虑在防火墙层面封禁该IP";

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(adminEmails);
            message.setSubject("[FrostS 安全告警] 登录异常检测 - IP " + clientIp);
            message.setText(String.format("""
                    FrostS 测试平台 - 登录异常告警
                    ================================
                    
                    告警类型：登录失败次数异常
                    告警时间：%s
                    
                    异常信息：
                      - 来源IP：%s
                      - 失败次数：%d 次/%d分钟
                      - %s
                    
                    建议操作：
                      1. 检查该IP是否为已知攻击源
                      2. 检查相关用户账号是否安全
                      3. 如需手动解封，可在系统设置中操作
                    
                    此邮件由系统自动发送，请勿回复。
                    """,
                    LocalDateTime.now().toString(),
                    clientIp,
                    failCount,
                    windowMinutes,
                    statusLine));

            mailSender.send(message);
            log.info("[LOGIN_ANOMALY] 告警邮件已发送至 {} 位管理员", adminEmails.length);
        } catch (Exception e) {
            log.warn("[LOGIN_ANOMALY] 发送告警邮件失败: {}", e.getMessage());
        }
    }

    private void sendAdminNotification(String clientIp, int failCount, String priority,
                                        boolean banned, int windowMinutes) {
        try {
            List<User> admins = userRepository.findByRoleCodeAndEnabled("ADMIN");
            List<Long> adminIds = admins.stream()
                    .map(User::getId)
                    .collect(Collectors.toList());

            if (adminIds.isEmpty()) return;

            String content = banned
                    ? String.format("IP %s 在%d分钟内登录失败 %d 次，已被自动封禁。", clientIp, windowMinutes, failCount)
                    : String.format("IP %s 在%d分钟内登录失败 %d 次，可能存在暴力破解攻击行为。",
                            clientIp, windowMinutes, failCount);

            NotificationEvent event = NotificationEvent.builder()
                    .source(this)
                    .type("LOGIN_ANOMALY")
                    .category("SECURITY")
                    .title(banned ? "IP已被自动封禁 - " + clientIp : "登录异常告警 - IP " + clientIp)
                    .content(content)
                    .priority(priority)
                    .recipientIds(adminIds)
                    .targetType("SYSTEM")
                    .targetUrl("/audit-logs")
                    .build();

            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.warn("[LOGIN_ANOMALY] 发送站内通知失败: {}", e.getMessage());
        }
    }

    private int getSettingAsInt(String key, int defaultValue) {
        try {
            return systemSettingRepository.findBySettingKeyAndIsDeletedFalse(key)
                    .map(setting -> {
                        try {
                            return Integer.parseInt(setting.getSettingValue());
                        } catch (NumberFormatException e) {
                            return defaultValue;
                        }
                    })
                    .orElse(defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
