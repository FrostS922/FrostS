package com.frosts.testplatform.service;

import com.frosts.testplatform.entity.LoginHistory;
import com.frosts.testplatform.entity.User;
import com.frosts.testplatform.repository.LoginHistoryRepository;
import com.frosts.testplatform.repository.RefreshTokenRepository;
import com.frosts.testplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityWeeklyReportService {

    private final LoginHistoryRepository loginHistoryRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginAnomalyAlertService loginAnomalyAlertService;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username:noreply@frosts.com}")
    private String fromEmail;

    public void generateAndSendWeeklyReport() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate weekStart = today.minusDays(7);
            LocalDateTime startDateTime = weekStart.atStartOfDay();
            LocalDateTime endDateTime = today.atStartOfDay();

            Context context = new Context();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            context.setVariable("startDate", weekStart.format(fmt));
            context.setVariable("endDate", today.format(fmt));

            List<LoginHistory> weekLogins = loginHistoryRepository
                    .findByLoginAtBetween(startDateTime, endDateTime);

            long successes = weekLogins.stream().filter(l -> l.getSuccess()).count();
            long failures = weekLogins.stream().filter(l -> !l.getSuccess()).count();
            context.setVariable("loginSuccesses", successes);
            context.setVariable("loginFailures", failures);

            long activeUsers = weekLogins.stream()
                    .filter(l -> l.getSuccess())
                    .map(LoginHistory::getUsername)
                    .distinct()
                    .count();
            context.setVariable("activeUsers", activeUsers);

            List<Map<String, Object>> dailyTrend = new ArrayList<>();
            for (int i = 6; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                LocalDateTime dayStart = date.atStartOfDay();
                LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
                List<LoginHistory> dayLogins = loginHistoryRepository
                        .findByLoginAtBetween(dayStart, dayEnd);
                Map<String, Object> point = new HashMap<>();
                point.put("date", date.format(fmt));
                point.put("successes", dayLogins.stream().filter(l -> l.getSuccess()).count());
                point.put("failures", dayLogins.stream().filter(l -> !l.getSuccess()).count());
                dailyTrend.add(point);
            }
            context.setVariable("dailyTrend", dailyTrend);

            List<Map<String, String>> bannedIps = loginAnomalyAlertService.getBannedIps();
            context.setVariable("bannedIpCount", bannedIps.size());
            context.setVariable("newBansThisWeek", failures);
            context.setVariable("bannedIps", bannedIps);

            long suspiciousIps = weekLogins.stream()
                    .filter(l -> !l.getSuccess())
                    .map(LoginHistory::getLoginIp)
                    .distinct()
                    .count();
            context.setVariable("suspiciousIpCount", suspiciousIps);
            context.setVariable("tokenAbuseCount", 0);

            List<User> allUsers = userRepository.findByIsDeletedFalse(org.springframework.data.domain.Pageable.unpaged()).getContent();
            long mfaEnabled = allUsers.stream()
                    .filter(u -> Boolean.TRUE.equals(u.getMfaEnabled()))
                    .count();
            double mfaRate = allUsers.isEmpty() ? 0 : (mfaEnabled * 100.0 / allUsers.size());
            context.setVariable("mfaEnableRate", String.format("%.1f%%", mfaRate));
            context.setVariable("passwordComplianceRate", "100.0%");

            long activeSessions = refreshTokenRepository
                    .findByExpiryDateAfterAndIsRevokedFalse(LocalDateTime.now()).size();
            context.setVariable("activeSessionCount", activeSessions);

            String html = templateEngine.process("email/security-weekly-report", context);

            List<User> admins = userRepository.findByRoleCodeAndEnabled("ADMIN");
            String[] adminEmails = admins.stream()
                    .map(User::getEmail)
                    .filter(email -> email != null && !email.isEmpty())
                    .toArray(String[]::new);

            if (adminEmails.length > 0) {
                sendHtmlEmail(html, adminEmails);
                log.info("[WEEKLY_REPORT] 安全周报已发送至 {} 位管理员", adminEmails.length);
            } else {
                log.warn("[WEEKLY_REPORT] 未找到管理员邮箱，跳过发送");
            }
        } catch (Exception e) {
            log.error("[WEEKLY_REPORT] 生成安全周报失败", e);
        }
    }

    public String generateReportHtml() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(7);
        LocalDateTime startDateTime = weekStart.atStartOfDay();
        LocalDateTime endDateTime = today.atStartOfDay();

        Context context = new Context();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        context.setVariable("startDate", weekStart.format(fmt));
        context.setVariable("endDate", today.format(fmt));

        List<LoginHistory> weekLogins = loginHistoryRepository
                .findByLoginAtBetween(startDateTime, endDateTime);
        context.setVariable("loginSuccesses", weekLogins.stream().filter(l -> l.getSuccess()).count());
        context.setVariable("loginFailures", weekLogins.stream().filter(l -> !l.getSuccess()).count());
        context.setVariable("activeUsers", weekLogins.stream().filter(l -> l.getSuccess())
                .map(LoginHistory::getUsername).distinct().count());
        context.setVariable("dailyTrend", Collections.emptyList());
        context.setVariable("bannedIpCount", 0);
        context.setVariable("newBansThisWeek", 0);
        context.setVariable("bannedIps", Collections.emptyList());
        context.setVariable("suspiciousIpCount", 0);
        context.setVariable("tokenAbuseCount", 0);
        context.setVariable("mfaEnableRate", "0%");
        context.setVariable("passwordComplianceRate", "100%");
        context.setVariable("activeSessionCount", 0);

        return templateEngine.process("email/security-weekly-report", context);
    }

    private void sendHtmlEmail(String html, String[] to) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("[FrostS 安全周报] " + LocalDate.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        helper.setText(html, true);
        mailSender.send(message);
    }
}
