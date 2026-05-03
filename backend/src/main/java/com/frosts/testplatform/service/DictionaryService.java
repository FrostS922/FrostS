package com.frosts.testplatform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frosts.testplatform.dto.dictionary.*;
import com.frosts.testplatform.entity.*;
import com.frosts.testplatform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DictionaryService {

    private final DictionaryTypeRepository typeRepository;
    private final DictionaryItemRepository itemRepository;
    private final DictionaryTypeRoleRepository typeRoleRepository;
    private final DictionaryLogRepository logRepository;
    private final ObjectMapper objectMapper;

    // ==================== 分类管理 ====================

    @Transactional(readOnly = true)
    public List<DictionaryTypeResponse> getTypeTree() {
        List<DictionaryType> allTypes = typeRepository.findByIsDeletedFalseOrderBySortOrderAsc();
        return buildTypeTree(allTypes);
    }

    @Transactional(readOnly = true)
    public DictionaryTypeResponse getTypeById(Long id) {
        DictionaryType type = findActiveType(id);
        return toTypeResponse(type, List.of());
    }

    public DictionaryTypeResponse createType(CreateDictionaryTypeRequest request) {
        String code = normalize(request.code());
        if (code == null) {
            throw new IllegalArgumentException("分类编码不能为空");
        }
        code = code.toUpperCase();

        if (typeRepository.existsByCodeAndIsDeletedFalse(code)) {
            throw new IllegalArgumentException("分类编码已存在: " + code);
        }

        DictionaryType type = new DictionaryType();
        type.setParentId(request.parentId());
        type.setCode(code);
        type.setName(requireText(request.name(), "分类名称不能为空"));
        type.setDescription(normalize(request.description()));
        type.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        type.setEnabled(request.enabled() != null ? request.enabled() : true);

        DictionaryType saved = typeRepository.save(type);
        logAction(saved.getId(), null, "CREATE", null, toJson(saved));

        return toTypeResponse(saved, List.of());
    }

    public DictionaryTypeResponse updateType(Long id, UpdateDictionaryTypeRequest request) {
        DictionaryType type = findActiveType(id);

        if (Boolean.TRUE.equals(type.getIsSystem())) {
            throw new IllegalStateException("系统内置分类不能修改");
        }

        String code = normalize(request.code());
        if (code != null) {
            String upperCode = code.toUpperCase();
            typeRepository.findByCodeAndIsDeletedFalse(upperCode)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("分类编码已存在: " + upperCode);
                    });
            type.setCode(upperCode);
        }

        String oldValue = toJson(type);

        type.setParentId(request.parentId());
        type.setName(requireText(request.name(), "分类名称不能为空"));
        type.setDescription(normalize(request.description()));
        type.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        if (request.enabled() != null) {
            type.setEnabled(request.enabled());
        }

        DictionaryType saved = typeRepository.save(type);
        logAction(saved.getId(), null, "UPDATE", oldValue, toJson(saved));

        return toTypeResponse(saved, List.of());
    }

    public void deleteType(Long id) {
        DictionaryType type = findActiveType(id);

        if (Boolean.TRUE.equals(type.getIsSystem())) {
            throw new IllegalStateException("系统内置分类不能删除");
        }

        long itemCount = itemRepository.countByTypeIdAndIsDeletedFalse(id);
        if (itemCount > 0) {
            throw new IllegalStateException("分类下存在枚举值，不能删除");
        }

        String oldValue = toJson(type);
        type.setIsDeleted(true);
        type.setEnabled(false);
        typeRepository.save(type);

        // 删除权限配置
        typeRoleRepository.deleteByTypeId(id);

        logAction(id, null, "DELETE", oldValue, null);
    }

    // ==================== 枚举值管理 ====================

    @Transactional(readOnly = true)
    public Page<DictionaryItemResponse> getItems(Long typeId, String keyword, Pageable pageable) {
        Page<DictionaryItem> items;
        if (keyword != null && !keyword.isEmpty()) {
            items = itemRepository.searchByTypeIdAndKeyword(typeId, keyword, pageable);
        } else {
            items = itemRepository.findByTypeIdAndIsDeletedFalse(typeId, pageable);
        }

        DictionaryType type = typeRepository.findByIdAndIsDeletedFalse(typeId).orElse(null);
        String typeCode = type != null ? type.getCode() : null;
        String typeName = type != null ? type.getName() : null;

        return items.map(item -> toItemResponse(item, typeId, typeCode, typeName));
    }

    @Transactional(readOnly = true)
    public List<DictionaryItemResponse> getItemsByTypeCode(String typeCode) {
        DictionaryType type = typeRepository.findByCodeAndIsDeletedFalse(typeCode)
                .orElseThrow(() -> new IllegalArgumentException("字典分类不存在: " + typeCode));

        List<DictionaryItem> items = itemRepository.findByTypeIdAndEnabledAndIsDeletedFalseOrderBySortOrderAsc(type.getId(), true);
        return items.stream()
                .map(item -> toItemResponse(item, type.getId(), type.getCode(), type.getName()))
                .toList();
    }

    public DictionaryItemResponse createItem(CreateDictionaryItemRequest request) {
        Long typeId = request.typeId();
        DictionaryType type = findActiveType(typeId);

        String code = normalize(request.code());
        if (code == null) {
            throw new IllegalArgumentException("枚举编码不能为空");
        }

        if (itemRepository.existsByTypeIdAndCodeAndIsDeletedFalse(typeId, code)) {
            throw new IllegalArgumentException("枚举编码已存在: " + code);
        }

        DictionaryItem item = new DictionaryItem();
        item.setTypeId(typeId);
        item.setCode(code);
        item.setName(requireText(request.name(), "枚举名称不能为空"));
        item.setValue(normalize(request.value()));
        item.setDescription(normalize(request.description()));
        item.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        item.setEnabled(request.enabled() != null ? request.enabled() : true);
        item.setIsDefault(request.isDefault() != null ? request.isDefault() : false);
        item.setColor(normalize(request.color()));

        DictionaryItem saved = itemRepository.save(item);
        logAction(typeId, saved.getId(), "CREATE", null, toJson(saved));

        return toItemResponse(saved, typeId, type.getCode(), type.getName());
    }

    public DictionaryItemResponse updateItem(Long id, UpdateDictionaryItemRequest request) {
        DictionaryItem item = findActiveItem(id);
        DictionaryType type = findActiveType(item.getTypeId());

        String oldValue = toJson(item);

        item.setName(requireText(request.name(), "枚举名称不能为空"));
        item.setValue(normalize(request.value()));
        item.setDescription(normalize(request.description()));
        item.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        if (request.enabled() != null) {
            item.setEnabled(request.enabled());
        }
        if (request.isDefault() != null) {
            item.setIsDefault(request.isDefault());
        }
        item.setColor(normalize(request.color()));

        DictionaryItem saved = itemRepository.save(item);
        logAction(type.getId(), saved.getId(), "UPDATE", oldValue, toJson(saved));

        return toItemResponse(saved, type.getId(), type.getCode(), type.getName());
    }

    public void deleteItem(Long id) {
        DictionaryItem item = findActiveItem(id);
        DictionaryType type = findActiveType(item.getTypeId());

        String oldValue = toJson(item);
        item.setIsDeleted(true);
        item.setEnabled(false);
        itemRepository.save(item);

        logAction(type.getId(), id, "DELETE", oldValue, null);
    }

    public DictionaryItemResponse updateItemStatus(Long id, boolean enabled) {
        DictionaryItem item = findActiveItem(id);
        DictionaryType type = findActiveType(item.getTypeId());

        String oldValue = toJson(item);
        item.setEnabled(enabled);
        DictionaryItem saved = itemRepository.save(item);

        logAction(type.getId(), id, enabled ? "ENABLE" : "DISABLE", oldValue, toJson(saved));

        return toItemResponse(saved, type.getId(), type.getCode(), type.getName());
    }

    // ==================== 权限管理 ====================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTypePermissions(Long typeId) {
        findActiveType(typeId);
        List<DictionaryTypeRole> typeRoles = typeRoleRepository.findByTypeId(typeId);

        return typeRoles.stream()
                .map(tr -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("roleId", tr.getRoleId());
                    map.put("permission", tr.getPermission());
                    return map;
                })
                .toList();
    }

    public void setTypePermissions(Long typeId, DictionaryTypePermissionRequest request) {
        findActiveType(typeId);

        // 删除旧权限
        typeRoleRepository.deleteByTypeId(typeId);

        // 添加新权限
        if (request.permissions() != null) {
            for (DictionaryTypePermissionRequest.PermissionItem perm : request.permissions()) {
                DictionaryTypeRole typeRole = new DictionaryTypeRole();
                typeRole.setTypeId(typeId);
                typeRole.setRoleId(perm.roleId());
                typeRole.setPermission(perm.permission().toUpperCase());
                typeRoleRepository.save(typeRole);
            }
        }
    }

    // ==================== 日志查询 ====================

    @Transactional(readOnly = true)
    public Page<DictionaryLogResponse> getLogs(Long typeId, String operator, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        Page<DictionaryLog> logs;
        if (typeId != null) {
            logs = logRepository.findByTypeIdOrderByOperatedAtDesc(typeId, pageable);
        } else if (operator != null && !operator.isEmpty()) {
            logs = logRepository.findByOperatorOrderByOperatedAtDesc(operator, pageable);
        } else if (startTime != null && endTime != null) {
            logs = logRepository.findByTimeRange(startTime, endTime, pageable);
        } else {
            logs = logRepository.findAll(pageable);
        }

        return logs.map(this::toLogResponse);
    }

    // ==================== 私有方法 ====================

    private DictionaryType findActiveType(Long id) {
        return typeRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("字典分类不存在: " + id));
    }

    private DictionaryItem findActiveItem(Long id) {
        return itemRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("字典项不存在: " + id));
    }

    private List<DictionaryTypeResponse> buildTypeTree(List<DictionaryType> allTypes) {
        Map<Long, List<DictionaryType>> childrenByParentId = allTypes.stream()
                .filter(type -> type.getParentId() != null)
                .collect(Collectors.groupingBy(DictionaryType::getParentId));

        Set<Long> allIds = allTypes.stream().map(DictionaryType::getId).collect(Collectors.toSet());

        return allTypes.stream()
                .filter(type -> type.getParentId() == null || !allIds.contains(type.getParentId()))
                .map(type -> toTypeResponse(type, childrenByParentId))
                .toList();
    }

    private DictionaryTypeResponse toTypeResponse(DictionaryType type, Map<Long, List<DictionaryType>> childrenByParentId) {
        List<DictionaryTypeResponse> children = childrenByParentId.getOrDefault(type.getId(), List.of())
                .stream()
                .map(child -> toTypeResponse(child, childrenByParentId))
                .toList();
        return toTypeResponse(type, children);
    }

    private DictionaryTypeResponse toTypeResponse(DictionaryType type, List<DictionaryTypeResponse> children) {
        return new DictionaryTypeResponse(
                type.getId(),
                type.getParentId(),
                type.getCode(),
                type.getName(),
                type.getDescription(),
                type.getSortOrder(),
                type.getEnabled(),
                type.getIsSystem(),
                children,
                type.getCreatedAt(),
                type.getUpdatedAt()
        );
    }

    private DictionaryItemResponse toItemResponse(DictionaryItem item, Long typeId, String typeCode, String typeName) {
        return new DictionaryItemResponse(
                item.getId(),
                typeId,
                typeCode,
                typeName,
                item.getCode(),
                item.getName(),
                item.getValue(),
                item.getDescription(),
                item.getSortOrder(),
                item.getEnabled(),
                item.getIsDefault(),
                item.getColor(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private DictionaryLogResponse toLogResponse(DictionaryLog log) {
        return new DictionaryLogResponse(
                log.getId(),
                log.getTypeId(),
                null, // typeCode 可以后续优化查询
                log.getItemId(),
                null, // itemCode 可以后续优化查询
                log.getAction(),
                log.getOldValue(),
                log.getNewValue(),
                log.getOperator(),
                log.getOperatedAt(),
                log.getIpAddress()
        );
    }

    private void logAction(Long typeId, Long itemId, String action, String oldValue, String newValue) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String operator = authentication != null ? authentication.getName() : "system";

        DictionaryLog log = new DictionaryLog();
        log.setTypeId(typeId);
        log.setItemId(itemId);
        log.setAction(action);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setOperator(operator);
        log.setOperatedAt(LocalDateTime.now());

        logRepository.save(log);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String requireText(String value, String message) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    @SneakyThrows
    private String toJson(Object obj) {
        return objectMapper.writeValueAsString(obj);
    }
}
