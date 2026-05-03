package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.dto.dictionary.*;
import com.frosts.testplatform.service.DictionaryExcelService;
import com.frosts.testplatform.service.DictionaryPermissionService;
import com.frosts.testplatform.service.DictionaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/dictionary")
@RequiredArgsConstructor
public class DictionaryController {

    private final DictionaryService dictionaryService;
    private final DictionaryExcelService excelService;
    private final DictionaryPermissionService permissionService;

    // ==================== 分类管理 ====================

    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<DictionaryTypeResponse>>> getTypeTree() {
        return ResponseEntity.ok(ApiResponse.success(dictionaryService.getTypeTree()));
    }

    @GetMapping("/types/{id}")
    public ResponseEntity<ApiResponse<DictionaryTypeResponse>> getTypeById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(dictionaryService.getTypeById(id)));
    }

    @PostMapping("/types")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DictionaryTypeResponse>> createType(
            @Valid @RequestBody CreateDictionaryTypeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(dictionaryService.createType(request)));
    }

    @PutMapping("/types/{id}")
    public ResponseEntity<ApiResponse<DictionaryTypeResponse>> updateType(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDictionaryTypeRequest request) {
        if (!permissionService.hasAdminPermission(id)) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, "没有权限修改此分类"));
        }
        return ResponseEntity.ok(ApiResponse.success(dictionaryService.updateType(id, request)));
    }

    @DeleteMapping("/types/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteType(@PathVariable Long id) {
        if (!permissionService.hasAdminPermission(id)) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, "没有权限删除此分类"));
        }
        dictionaryService.deleteType(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ==================== 枚举值管理 ====================

    @GetMapping("/types/{typeId}/items")
    public ResponseEntity<ApiResponse<List<DictionaryItemResponse>>> getItems(
            @PathVariable Long typeId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (!permissionService.hasReadPermission(typeId)) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, "没有权限查看此分类"));
        }
        Page<DictionaryItemResponse> items = dictionaryService.getItems(typeId, keyword,
                PageRequest.of(page, size, Sort.by("sortOrder").ascending()));
        return ResponseEntity.ok(ApiResponse.success(items.getContent(), items.getTotalElements()));
    }

    @GetMapping("/types/code/{typeCode}/items")
    public ResponseEntity<ApiResponse<List<DictionaryItemResponse>>> getItemsByTypeCode(
            @PathVariable String typeCode) {
        return ResponseEntity.ok(ApiResponse.success(dictionaryService.getItemsByTypeCode(typeCode)));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<DictionaryItemResponse>> createItem(
            @Valid @RequestBody CreateDictionaryItemRequest request) {
        if (!permissionService.hasWritePermission(request.typeId())) {
            return ResponseEntity.status(403).body(ApiResponse.error(403, "没有权限添加枚举值"));
        }
        return ResponseEntity.ok(ApiResponse.success(dictionaryService.createItem(request)));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<ApiResponse<DictionaryItemResponse>> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDictionaryItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success(dictionaryService.updateItem(id, request)));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(@PathVariable Long id) {
        dictionaryService.deleteItem(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/items/{id}/status")
    public ResponseEntity<ApiResponse<DictionaryItemResponse>> updateItemStatus(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        return ResponseEntity.ok(ApiResponse.success(dictionaryService.updateItemStatus(id, enabled)));
    }

    // ==================== 权限管理 ====================

    @GetMapping("/types/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTypePermissions(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(dictionaryService.getTypePermissions(id)));
    }

    @PutMapping("/types/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> setTypePermissions(
            @PathVariable Long id,
            @Valid @RequestBody DictionaryTypePermissionRequest request) {
        dictionaryService.setTypePermissions(id, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ==================== 导入导出 ====================

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> importExcel(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = excelService.importFromExcel(file);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "导入失败: " + e.getMessage()));
        }
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel() {
        try {
            byte[] data = excelService.exportToExcel();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=dictionary_export.xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(data);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ==================== 操作日志 ====================

    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<DictionaryLogResponse>>> getLogs(
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<DictionaryLogResponse> logs = dictionaryService.getLogs(typeId, operator, startTime, endTime,
                PageRequest.of(page, size, Sort.by("operatedAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(logs.getContent(), logs.getTotalElements()));
    }
}
