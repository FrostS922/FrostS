package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.entity.AuditLog;
import com.frosts.testplatform.repository.AuditLogRepository;
import com.frosts.testplatform.util.ExportUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String operator) {
        Sort sort = Sort.by(Sort.Direction.DESC, "operatedAt");
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AuditLog> result;
        if (operator != null && !operator.isBlank()) {
            result = auditLogRepository.findByOperatorOrderByOperatedAtDesc(operator, pageable);
        } else if (action != null && !action.isBlank()) {
            result = auditLogRepository.findByActionOrderByOperatedAtDesc(action, pageable);
        } else {
            result = auditLogRepository.findAllByOrderByOperatedAtDesc(pageable);
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/export/excel")
    public void exportExcel(
            HttpServletResponse response,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String operator) throws IOException {
        List<AuditLog> logs = findFilteredLogs(action, operator);

        String filename = URLEncoder.encode("审计日志_" + java.time.LocalDate.now() + ".xlsx", StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);

        String[] headers = {"ID", "操作时间", "操作类型", "目标", "目标ID", "操作人", "操作IP", "旧值", "新值", "描述"};
        List<String[]> rows = logs.stream().map(log -> new String[]{
                String.valueOf(log.getId()),
                log.getOperatedAt() != null ? log.getOperatedAt().format(FORMATTER) : "",
                log.getAction() != null ? log.getAction() : "",
                log.getTarget() != null ? log.getTarget() : "",
                log.getTargetId() != null ? log.getTargetId() : "",
                log.getOperator() != null ? log.getOperator() : "",
                log.getOperatorIp() != null ? log.getOperatorIp() : "",
                log.getOldValue() != null ? log.getOldValue() : "",
                log.getNewValue() != null ? log.getNewValue() : "",
                log.getDescription() != null ? log.getDescription() : ""
        }).toList();

        response.getOutputStream().write(ExportUtils.toExcel(headers, rows));
    }

    @GetMapping("/export/csv")
    public void exportCsv(
            HttpServletResponse response,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String operator) throws IOException {
        List<AuditLog> logs = findFilteredLogs(action, operator);

        String filename = URLEncoder.encode("审计日志_" + java.time.LocalDate.now() + ".csv", StandardCharsets.UTF_8);
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);

        String[] headers = {"ID", "操作时间", "操作类型", "目标", "目标ID", "操作人", "操作IP", "旧值", "新值", "描述"};
        List<String[]> rows = logs.stream().map(log -> new String[]{
                String.valueOf(log.getId()),
                log.getOperatedAt() != null ? log.getOperatedAt().format(FORMATTER) : "",
                log.getAction() != null ? log.getAction() : "",
                log.getTarget() != null ? log.getTarget() : "",
                log.getTargetId() != null ? log.getTargetId() : "",
                log.getOperator() != null ? log.getOperator() : "",
                log.getOperatorIp() != null ? log.getOperatorIp() : "",
                log.getOldValue() != null ? log.getOldValue() : "",
                log.getNewValue() != null ? log.getNewValue() : "",
                log.getDescription() != null ? log.getDescription() : ""
        }).toList();

        response.getOutputStream().write(ExportUtils.toCsv(headers, rows));
    }

    private List<AuditLog> findFilteredLogs(String action, String operator) {
        Sort sort = Sort.by(Sort.Direction.DESC, "operatedAt");
        Pageable allPage = PageRequest.of(0, 10000, sort);

        if (operator != null && !operator.isBlank()) {
            return auditLogRepository.findByOperatorOrderByOperatedAtDesc(operator, allPage).getContent();
        } else if (action != null && !action.isBlank()) {
            return auditLogRepository.findByActionOrderByOperatedAtDesc(action, allPage).getContent();
        } else {
            return auditLogRepository.findAllByOrderByOperatedAtDesc(allPage).getContent();
        }
    }

}
