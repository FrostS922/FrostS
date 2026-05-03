package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.dto.errorreport.BatchDeleteErrorLogsRequest;
import com.frosts.testplatform.dto.errorreport.ErrorAggregationResponse;
import com.frosts.testplatform.dto.errorreport.ErrorLogResponse;
import com.frosts.testplatform.dto.errorreport.ErrorOverviewResponse;
import com.frosts.testplatform.dto.errorreport.ErrorReportRequest;
import com.frosts.testplatform.dto.errorreport.ErrorTrendResponse;
import com.frosts.testplatform.service.ErrorReportService;
import com.frosts.testplatform.service.FileStorageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/error-report")
@RequiredArgsConstructor
public class ErrorReportController {

    private final ErrorReportService errorReportService;
    private final FileStorageService fileStorageService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> reportError(@Valid @RequestBody ErrorReportRequest request) {
        errorReportService.reportError(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ErrorLogResponse>>> getErrorLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "created_at"));
        Page<ErrorLogResponse> result = errorReportService.getErrorLogs(keyword, startTime, endTime, pageable);
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), result.getTotalElements()));
    }

    @GetMapping("/aggregated")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ErrorAggregationResponse>>> getAggregatedErrors(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ErrorAggregationResponse> result = errorReportService.getAggregatedErrors(keyword, startTime, endTime, pageable);
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), result.getTotalElements()));
    }

    @GetMapping("/by-message")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ErrorLogResponse>>> getErrorsByMessage(
            @RequestParam String errorMessage,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ErrorLogResponse> result = errorReportService.getErrorsByMessage(errorMessage, pageable);
        return ResponseEntity.ok(ApiResponse.success(result.getContent(), result.getTotalElements()));
    }

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ErrorOverviewResponse>> getErrorOverview() {
        return ResponseEntity.ok(ApiResponse.success(errorReportService.getErrorOverview()));
    }

    @GetMapping("/trend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ErrorTrendResponse>> getErrorTrend(
            @RequestParam(required = false) Integer days) {
        return ResponseEntity.ok(ApiResponse.success(errorReportService.getErrorTrend(days)));
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportErrorLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "csv") String format) {
        byte[] data = errorReportService.exportErrorLogs(keyword, startTime, endTime, format);
        String filename = "error-logs-" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));

        if ("excel".equalsIgnoreCase(format)) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename + ".xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(data);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename + ".csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(data);
    }

    @PostMapping("/sourcemaps")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadSourceMap(
            @RequestParam("file") MultipartFile file,
            @RequestParam @NotBlank String version) {
        String url = fileStorageService.storeSourceMap(file, version);
        return ResponseEntity.ok(ApiResponse.success(Map.of("url", url, "version", version)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteErrorLog(@PathVariable Long id) {
        errorReportService.deleteErrorLog(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> batchDeleteErrorLogs(@Valid @RequestBody BatchDeleteErrorLogsRequest request) {
        errorReportService.batchDeleteErrorLogs(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
