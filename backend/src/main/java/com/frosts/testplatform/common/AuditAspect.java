package com.frosts.testplatform.common;

import com.frosts.testplatform.entity.AuditLog;
import com.frosts.testplatform.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    @Around("@annotation(com.frosts.testplatform.common.Auditable)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Auditable auditable = method.getAnnotation(Auditable.class);

        String operator = getCurrentUsername();
        String operatorIp = getClientIp();

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable throwable) {
            recordAudit(auditable, operator, operatorIp, null, null, "操作失败: " + throwable.getMessage());
            throw throwable;
        }

        String description = auditable.description();
        if (description.isEmpty()) {
            description = auditable.action();
        }

        recordAudit(auditable, operator, operatorIp, null, null, description);

        return result;
    }

    private void recordAudit(Auditable auditable, String operator, String operatorIp,
                             String oldValue, String newValue, String description) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(auditable.action())
                    .target(auditable.target())
                    .operator(operator)
                    .operatorIp(operatorIp)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .operatedAt(LocalDateTime.now())
                    .description(description)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("[AUDIT] 记录审计日志失败: {}", e.getMessage());
        }
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "anonymous";
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return "unknown";

            HttpServletRequest request = attributes.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        } catch (Exception e) {
            return "unknown";
        }
    }
}
