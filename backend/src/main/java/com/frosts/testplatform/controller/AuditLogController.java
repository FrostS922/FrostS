package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.dto.auditlog.AuditLogResponse;
import com.frosts.testplatform.entity.AuditLog;
import com.frosts.testplatform.service.AuditLogService;
import com.frosts.testplatform.util.ExportUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "审计日志", description = "操作审计日志查询与导出")
public class AuditLogController {

    private final AuditLogService auditLogService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping
    @Operation(summary = "分页查询审计日志")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(
            @RequestParam(defaultValue = "0") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页数量") int size,
            @RequestParam(required = false) @Parameter(description = "操作类型") String action,
            @RequestParam(required = false) @Parameter(description = "操作人") String operator) {
        Sort sort = Sort.by(Sort.Direction.DESC, "operatedAt");
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<AuditLogResponse> result = auditLogService.getAuditLogs(action, operator, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/export/excel")
    @Operation(summary = "导出审计日志Excel")
    public void exportExcel(
            HttpServletResponse response,
            @RequestParam(required = false) @Parameter(description = "操作类型") String action,
            @RequestParam(required = false) @Parameter(description = "操作人") String operator) throws IOException {
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
    @Operation(summary = "导出审计日志CSV")
    public void exportCsv(
            HttpServletResponse response,
            @RequestParam(required = false) @Parameter(description = "操作类型") String action,
            @RequestParam(required = false) @Parameter(description = "操作人") String operator) throws IOException {
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
        return auditLogService.getAuditLogsForExport(action, operator, allPage);
    }
}
