# 数据字典管理模块实施计划

> **目标:** 为测试管理平台实现完整的数据字典管理功能模块

**架构:** 后端Spring Boot 3.2 + JPA + PostgreSQL + Redis，前端React + Ant Design + TypeScript。新增4张数据库表，实现多级树形分类、枚举值CRUD、按分类权限控制、Excel导入导出、Redis缓存和操作日志。

**技术栈:** Spring Boot, Spring Data JPA, Spring Security, Redis, Apache POI, React, Ant Design, ProComponents

---

## Task 1: 添加数据库迁移脚本

**Files:**
- Create: `backend/src/main/resources/db/migration/V3__add_dictionary_tables.sql`

**Step 1: 编写迁移脚本**

```sql
-- 字典分类表
CREATE TABLE IF NOT EXISTS sys_dictionary_type (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT REFERENCES sys_dictionary_type(id),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    description TEXT,
    sort_order INT DEFAULT 0,
    enabled BOOLEAN DEFAULT TRUE,
    is_system BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    is_deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_dict_type_parent ON sys_dictionary_type(parent_id);
CREATE INDEX idx_dict_type_code ON sys_dictionary_type(code);
CREATE INDEX idx_dict_type_enabled ON sys_dictionary_type(enabled);

-- 字典枚举值表
CREATE TABLE IF NOT EXISTS sys_dictionary_item (
    id BIGSERIAL PRIMARY KEY,
    type_id BIGINT NOT NULL REFERENCES sys_dictionary_type(id),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(50) NOT NULL,
    value VARCHAR(100),
    description TEXT,
    sort_order INT DEFAULT 0,
    enabled BOOLEAN DEFAULT TRUE,
    is_default BOOLEAN DEFAULT FALSE,
    color VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    is_deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(type_id, code)
);

CREATE INDEX idx_dict_item_type ON sys_dictionary_item(type_id);
CREATE INDEX idx_dict_item_code ON sys_dictionary_item(code);
CREATE INDEX idx_dict_item_enabled ON sys_dictionary_item(enabled);

-- 字典分类权限表
CREATE TABLE IF NOT EXISTS sys_dictionary_type_role (
    id BIGSERIAL PRIMARY KEY,
    type_id BIGINT NOT NULL REFERENCES sys_dictionary_type(id),
    role_id BIGINT NOT NULL REFERENCES sys_role(id),
    permission VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(type_id, role_id)
);

CREATE INDEX idx_dict_perm_type ON sys_dictionary_type_role(type_id);
CREATE INDEX idx_dict_perm_role ON sys_dictionary_type_role(role_id);

-- 操作日志表
CREATE TABLE IF NOT EXISTS sys_dictionary_log (
    id BIGSERIAL PRIMARY KEY,
    type_id BIGINT,
    item_id BIGINT,
    action VARCHAR(20) NOT NULL,
    old_value JSONB,
    new_value JSONB,
    operator VARCHAR(50) NOT NULL,
    operated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(50)
);

CREATE INDEX idx_dict_log_type ON sys_dictionary_log(type_id);
CREATE INDEX idx_dict_log_item ON sys_dictionary_log(item_id);
CREATE INDEX idx_dict_log_time ON sys_dictionary_log(operated_at);
```

**Step 2: 验证脚本**

启动应用，检查Flyway是否成功执行迁移。

**Step 3: Commit**

```bash
git add backend/src/main/resources/db/migration/V3__add_dictionary_tables.sql
git commit -m "feat: add dictionary management database tables"
```

---

## Task 2: 创建实体类

**Files:**
- Create: `backend/src/main/java/com/frosts/testplatform/entity/DictionaryType.java`
- Create: `backend/src/main/java/com/frosts/testplatform/entity/DictionaryItem.java`
- Create: `backend/src/main/java/com/frosts/testplatform/entity/DictionaryTypeRole.java`
- Create: `backend/src/main/java/com/frosts/testplatform/entity/DictionaryLog.java`

**Step 1: 创建 DictionaryType 实体**

```java
package com.frosts.testplatform.entity;

import com.frosts.testplatform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_dictionary_type")
public class DictionaryType extends BaseEntity {

    @Column(name = "parent_id")
    private Long parentId;

    @Column(unique = true, nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "is_system")
    private Boolean isSystem = false;

    @Transient
    private List<DictionaryType> children = new ArrayList<>();
}
```

**Step 2: 创建 DictionaryItem 实体**

```java
package com.frosts.testplatform.entity;

import com.frosts.testplatform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_dictionary_item")
public class DictionaryItem extends BaseEntity {

    @Column(name = "type_id", nullable = false)
    private Long typeId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 100)
    private String value;

    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(length = 20)
    private String color;
}
```

**Step 3: 创建 DictionaryTypeRole 实体**

```java
package com.frosts.testplatform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_dictionary_type_role")
public class DictionaryTypeRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_id", nullable = false)
    private Long typeId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(nullable = false, length = 20)
    private String permission;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
```

**Step 4: 创建 DictionaryLog 实体**

```java
package com.frosts.testplatform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_dictionary_log")
public class DictionaryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_id")
    private Long typeId;

    @Column(name = "item_id")
    private Long itemId;

    @Column(nullable = false, length = 20)
    private String action;

    @Column(name = "old_value", columnDefinition = "jsonb")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "jsonb")
    private String newValue;

    @Column(nullable = false, length = 50)
    private String operator;

    @Column(name = "operated_at")
    private LocalDateTime operatedAt = LocalDateTime.now();

    @Column(name = "ip_address", length = 50)
    private String ipAddress;
}
```

**Step 5: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/entity/
git commit -m "feat: add dictionary management entities"
```

---

## Task 3: 创建Repository接口

**Files:**
- Create: `backend/src/main/java/com/frosts/testplatform/repository/DictionaryTypeRepository.java`
- Create: `backend/src/main/java/com/frosts/testplatform/repository/DictionaryItemRepository.java`
- Create: `backend/src/main/java/com/frosts/testplatform/repository/DictionaryTypeRoleRepository.java`
- Create: `backend/src/main/java/com/frosts/testplatform/repository/DictionaryLogRepository.java`

**Step 1: 创建 DictionaryTypeRepository**

```java
package com.frosts.testplatform.repository;

import com.frosts.testplatform.entity.DictionaryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DictionaryTypeRepository extends JpaRepository<DictionaryType, Long> {

    Optional<DictionaryType> findByCodeAndIsDeletedFalse(String code);

    Optional<DictionaryType> findByIdAndIsDeletedFalse(Long id);

    List<DictionaryType> findByIsDeletedFalseOrderBySortOrderAsc();

    List<DictionaryType> findByParentIdAndIsDeletedFalseOrderBySortOrderAsc(Long parentId);

    boolean existsByCodeAndIsDeletedFalse(String code);

    @Query("SELECT dt FROM DictionaryType dt WHERE dt.isDeleted = false AND (dt.code LIKE %:keyword% OR dt.name LIKE %:keyword%)")
    List<DictionaryType> searchByKeyword(@Param("keyword") String keyword);
}
```

**Step 2: 创建 DictionaryItemRepository**

```java
package com.frosts.testplatform.repository;

import com.frosts.testplatform.entity.DictionaryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DictionaryItemRepository extends JpaRepository<DictionaryItem, Long> {

    Optional<DictionaryItem> findByIdAndIsDeletedFalse(Long id);

    List<DictionaryItem> findByTypeIdAndIsDeletedFalseOrderBySortOrderAsc(Long typeId);

    List<DictionaryItem> findByTypeIdAndEnabledAndIsDeletedFalseOrderBySortOrderAsc(Long typeId, Boolean enabled);

    Page<DictionaryItem> findByTypeIdAndIsDeletedFalse(Long typeId, Pageable pageable);

    boolean existsByTypeIdAndCodeAndIsDeletedFalse(Long typeId, String code);

    @Query("SELECT di FROM DictionaryItem di WHERE di.typeId = :typeId AND di.isDeleted = false AND (di.code LIKE %:keyword% OR di.name LIKE %:keyword%)")
    Page<DictionaryItem> searchByTypeIdAndKeyword(@Param("typeId") Long typeId, @Param("keyword") String keyword, Pageable pageable);

    long countByTypeIdAndIsDeletedFalse(Long typeId);
}
```

**Step 3: 创建 DictionaryTypeRoleRepository**

```java
package com.frosts.testplatform.repository;

import com.frosts.testplatform.entity.DictionaryTypeRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DictionaryTypeRoleRepository extends JpaRepository<DictionaryTypeRole, Long> {

    List<DictionaryTypeRole> findByTypeId(Long typeId);

    List<DictionaryTypeRole> findByRoleId(Long roleId);

    Optional<DictionaryTypeRole> findByTypeIdAndRoleId(Long typeId, Long roleId);

    void deleteByTypeId(Long typeId);
}
```

**Step 4: 创建 DictionaryLogRepository**

```java
package com.frosts.testplatform.repository;

import com.frosts.testplatform.entity.DictionaryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface DictionaryLogRepository extends JpaRepository<DictionaryLog, Long> {

    Page<DictionaryLog> findByTypeIdOrderByOperatedAtDesc(Long typeId, Pageable pageable);

    @Query("SELECT dl FROM DictionaryLog dl WHERE dl.operatedAt BETWEEN :startTime AND :endTime ORDER BY dl.operatedAt DESC")
    Page<DictionaryLog> findByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, Pageable pageable);

    Page<DictionaryLog> findByOperatorOrderByOperatedAtDesc(String operator, Pageable pageable);
}
```

**Step 5: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/repository/
git commit -m "feat: add dictionary management repositories"
```

---

## Task 4: 创建DTO类

**Files:**
- Create: `backend/src/main/java/com/frosts/testplatform/dto/dictionary/CreateDictionaryTypeRequest.java`
- Create: `backend/src/main/java/com/frosts/testplatform/dto/dictionary/UpdateDictionaryTypeRequest.java`
- Create: `backend/src/main/java/com/frosts/testplatform/dto/dictionary/DictionaryTypeResponse.java`
- Create: `backend/src/main/java/com/frosts/testplatform/dto/dictionary/CreateDictionaryItemRequest.java`
- Create: `backend/src/main/java/com/frosts/testplatform/dto/dictionary/UpdateDictionaryItemRequest.java`
- Create: `backend/src/main/java/com/frosts/testplatform/dto/dictionary/DictionaryItemResponse.java`
- Create: `backend/src/main/java/com/frosts/testplatform/dto/dictionary/DictionaryTypePermissionRequest.java`
- Create: `backend/src/main/java/com/frosts/testplatform/dto/dictionary/DictionaryLogResponse.java`

**Step 1: 创建请求/响应DTO (使用Java Records)**

```java
package com.frosts.testplatform.dto.dictionary;

public record CreateDictionaryTypeRequest(
    Long parentId,
    String code,
    String name,
    String description,
    Integer sortOrder,
    Boolean enabled
) {}
```

```java
package com.frosts.testplatform.dto.dictionary;

public record UpdateDictionaryTypeRequest(
    Long parentId,
    String code,
    String name,
    String description,
    Integer sortOrder,
    Boolean enabled
) {}
```

```java
package com.frosts.testplatform.dto.dictionary;

import java.time.LocalDateTime;
import java.util.List;

public record DictionaryTypeResponse(
    Long id,
    Long parentId,
    String code,
    String name,
    String description,
    Integer sortOrder,
    Boolean enabled,
    Boolean isSystem,
    List<DictionaryTypeResponse> children,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

```java
package com.frosts.testplatform.dto.dictionary;

public record CreateDictionaryItemRequest(
    Long typeId,
    String code,
    String name,
    String value,
    String description,
    Integer sortOrder,
    Boolean enabled,
    Boolean isDefault,
    String color
) {}
```

```java
package com.frosts.testplatform.dto.dictionary;

public record UpdateDictionaryItemRequest(
    String code,
    String name,
    String value,
    String description,
    Integer sortOrder,
    Boolean enabled,
    Boolean isDefault,
    String color
) {}
```

```java
package com.frosts.testplatform.dto.dictionary;

import java.time.LocalDateTime;

public record DictionaryItemResponse(
    Long id,
    Long typeId,
    String typeCode,
    String typeName,
    String code,
    String name,
    String value,
    String description,
    Integer sortOrder,
    Boolean enabled,
    Boolean isDefault,
    String color,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

```java
package com.frosts.testplatform.dto.dictionary;

import java.util.List;

public record DictionaryTypePermissionRequest(
    List<PermissionItem> permissions
) {
    public record PermissionItem(
        Long roleId,
        String permission
    ) {}
}
```

```java
package com.frosts.testplatform.dto.dictionary;

import java.time.LocalDateTime;

public record DictionaryLogResponse(
    Long id,
    Long typeId,
    String typeCode,
    Long itemId,
    String itemCode,
    String action,
    String oldValue,
    String newValue,
    String operator,
    LocalDateTime operatedAt,
    String ipAddress
) {}
```

**Step 2: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/dto/dictionary/
git commit -m "feat: add dictionary management DTOs"
```

---

## Task 5: 添加Apache POI依赖

**Files:**
- Modify: `backend/pom.xml`

**Step 1: 添加依赖**

```xml
<!-- Apache POI for Excel import/export -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

**Step 2: Commit**

```bash
git add backend/pom.xml
git commit -m "feat: add Apache POI dependency for Excel support"
```

---

## Task 6: 创建权限校验服务

**Files:**
- Create: `backend/src/main/java/com/frosts/testplatform/service/DictionaryPermissionService.java`

**Step 1: 实现权限校验服务**

```java
package com.frosts.testplatform.service;

import com.frosts.testplatform.entity.DictionaryType;
import com.frosts.testplatform.entity.DictionaryTypeRole;
import com.frosts.testplatform.entity.Role;
import com.frosts.testplatform.entity.User;
import com.frosts.testplatform.repository.DictionaryTypeRepository;
import com.frosts.testplatform.repository.DictionaryTypeRoleRepository;
import com.frosts.testplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DictionaryPermissionService {

    private final DictionaryTypeRoleRepository typeRoleRepository;
    private final DictionaryTypeRepository typeRepository;
    private final UserRepository userRepository;

    public boolean hasPermission(Long typeId, String requiredPermission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return false;
        }

        // ADMIN角色拥有所有权限
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> "ADMIN".equals(role.getCode()));
        if (isAdmin) {
            return true;
        }

        Set<Long> userRoleIds = user.getRoles().stream()
                .map(Role::getId)
                .collect(java.util.HashSet::new);

        // 获取该分类及其父分类的权限配置
        Map<Long, String> effectivePermissions = getEffectivePermissions(typeId);

        // 检查用户角色是否有足够权限
        for (Map.Entry<Long, String> entry : effectivePermissions.entrySet()) {
            if (userRoleIds.contains(entry.getKey())) {
                String permission = entry.getValue();
                if (hasSufficientPermission(permission, requiredPermission)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean hasReadPermission(Long typeId) {
        return hasPermission(typeId, "READ");
    }

    public boolean hasWritePermission(Long typeId) {
        return hasPermission(typeId, "WRITE");
    }

    public boolean hasAdminPermission(Long typeId) {
        return hasPermission(typeId, "ADMIN");
    }

    private Map<Long, String> getEffectivePermissions(Long typeId) {
        Map<Long, String> permissions = new HashMap<>();
        Set<Long> visited = new HashSet<>();
        Long currentId = typeId;

        while (currentId != null && !visited.contains(currentId)) {
            visited.add(currentId);
            List<DictionaryTypeRole> typeRoles = typeRoleRepository.findByTypeId(currentId);

            for (DictionaryTypeRole typeRole : typeRoles) {
                permissions.putIfAbsent(typeRole.getRoleId(), typeRole.getPermission());
            }

            Optional<DictionaryType> parent = typeRepository.findByIdAndIsDeletedFalse(currentId)
                    .map(type -> type.getParentId() != null ? 
                            typeRepository.findByIdAndIsDeletedFalse(type.getParentId()).orElse(null) : null);
            currentId = parent.map(DictionaryType::getId).orElse(null);
        }

        return permissions;
    }

    private boolean hasSufficientPermission(String userPermission, String requiredPermission) {
        int userLevel = getPermissionLevel(userPermission);
        int requiredLevel = getPermissionLevel(requiredPermission);
        return userLevel >= requiredLevel;
    }

    private int getPermissionLevel(String permission) {
        return switch (permission != null ? permission.toUpperCase() : "") {
            case "ADMIN" -> 3;
            case "WRITE" -> 2;
            case "READ" -> 1;
            default -> 0;
        };
    }
}
```

**Step 2: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/service/DictionaryPermissionService.java
git commit -m "feat: add dictionary permission service"
```

---

## Task 7: 创建核心业务服务

**Files:**
- Create: `backend/src/main/java/com/frosts/testplatform/service/DictionaryService.java`

**Step 1: 实现核心业务逻辑**

```java
package com.frosts.testplatform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frosts.testplatform.dto.dictionary.*;
import com.frosts.testplatform.entity.*;
import com.frosts.testplatform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
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
    private final DictionaryPermissionService permissionService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_TYPE_PREFIX = "dictionary:type:";
    private static final String CACHE_ITEMS_PREFIX = "dictionary:items:";
    private static final String CACHE_TREE = "dictionary:tree";

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
            throw new RuntimeException("分类编码不能为空");
        }
        code = code.toUpperCase();

        if (typeRepository.existsByCodeAndIsDeletedFalse(code)) {
            throw new RuntimeException("分类编码已存在: " + code);
        }

        DictionaryType type = new DictionaryType();
        type.setParentId(request.parentId());
        type.setCode(code);
        type.setName(requireText(request.name(), "分类名称不能为空"));
        type.setDescription(normalize(request.description()));
        type.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        type.setEnabled(request.enabled() != null ? request.enabled() : true);

        DictionaryType saved = typeRepository.save(type);
        clearTypeCache();
        logAction(saved.getId(), null, "CREATE", null, toJson(saved));

        return toTypeResponse(saved, List.of());
    }

    public DictionaryTypeResponse updateType(Long id, UpdateDictionaryTypeRequest request) {
        DictionaryType type = findActiveType(id);

        if (Boolean.TRUE.equals(type.getIsSystem())) {
            throw new RuntimeException("系统内置分类不能修改");
        }

        String code = normalize(request.code());
        if (code != null) {
            code = code.toUpperCase();
            typeRepository.findByCodeAndIsDeletedFalse(code)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new RuntimeException("分类编码已存在: " + code);
                    });
            type.setCode(code);
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
        clearTypeCache();
        clearItemsCache(saved.getCode());
        logAction(saved.getId(), null, "UPDATE", oldValue, toJson(saved));

        return toTypeResponse(saved, List.of());
    }

    public void deleteType(Long id) {
        DictionaryType type = findActiveType(id);

        if (Boolean.TRUE.equals(type.getIsSystem())) {
            throw new RuntimeException("系统内置分类不能删除");
        }

        long itemCount = itemRepository.countByTypeIdAndIsDeletedFalse(id);
        if (itemCount > 0) {
            throw new RuntimeException("分类下存在枚举值，不能删除");
        }

        String oldValue = toJson(type);
        type.setIsDeleted(true);
        type.setEnabled(false);
        typeRepository.save(type);

        // 删除权限配置
        typeRoleRepository.deleteByTypeId(id);

        clearTypeCache();
        clearItemsCache(type.getCode());
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
        String cacheKey = CACHE_ITEMS_PREFIX + typeCode;
        @SuppressWarnings("unchecked")
        List<DictionaryItemResponse> cached = (List<DictionaryItemResponse>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        DictionaryType type = typeRepository.findByCodeAndIsDeletedFalse(typeCode)
                .orElseThrow(() -> new RuntimeException("字典分类不存在: " + typeCode));

        List<DictionaryItem> items = itemRepository.findByTypeIdAndEnabledAndIsDeletedFalseOrderBySortOrderAsc(type.getId(), true);
        List<DictionaryItemResponse> responses = items.stream()
                .map(item -> toItemResponse(item, type.getId(), type.getCode(), type.getName()))
                .toList();

        redisTemplate.opsForValue().set(cacheKey, responses, java.time.Duration.ofMinutes(30));
        return responses;
    }

    public DictionaryItemResponse createItem(CreateDictionaryItemRequest request) {
        Long typeId = request.typeId();
        DictionaryType type = findActiveType(typeId);

        String code = normalize(request.code());
        if (code == null) {
            throw new RuntimeException("枚举编码不能为空");
        }

        if (itemRepository.existsByTypeIdAndCodeAndIsDeletedFalse(typeId, code)) {
            throw new RuntimeException("枚举编码已存在: " + code);
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
        clearItemsCache(type.getCode());
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
        clearItemsCache(type.getCode());
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

        clearItemsCache(type.getCode());
        logAction(type.getId(), id, "DELETE", oldValue, null);
    }

    public DictionaryItemResponse updateItemStatus(Long id, boolean enabled) {
        DictionaryItem item = findActiveItem(id);
        DictionaryType type = findActiveType(item.getTypeId());

        String oldValue = toJson(item);
        item.setEnabled(enabled);
        DictionaryItem saved = itemRepository.save(item);

        clearItemsCache(type.getCode());
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
        } else if (operator != null) {
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
                .orElseThrow(() -> new RuntimeException("字典分类不存在: " + id));
    }

    private DictionaryItem findActiveItem(Long id) {
        return itemRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("字典项不存在: " + id));
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

    private void clearTypeCache() {
        redisTemplate.delete(CACHE_TREE);
    }

    private void clearItemsCache(String typeCode) {
        redisTemplate.delete(CACHE_ITEMS_PREFIX + typeCode);
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
            throw new RuntimeException(message);
        }
        return normalized;
    }

    @SneakyThrows
    private String toJson(Object obj) {
        return objectMapper.writeValueAsString(obj);
    }
}
```

**Step 2: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/service/DictionaryService.java
git commit -m "feat: add dictionary core service with CRUD, cache and logging"
```

---

## Task 8: 创建Excel导入导出服务

**Files:**
- Create: `backend/src/main/java/com/frosts/testplatform/service/DictionaryExcelService.java`

**Step 1: 实现Excel导入导出**

```java
package com.frosts.testplatform.service;

import com.frosts.testplatform.entity.DictionaryItem;
import com.frosts.testplatform.entity.DictionaryType;
import com.frosts.testplatform.repository.DictionaryItemRepository;
import com.frosts.testplatform.repository.DictionaryTypeRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DictionaryExcelService {

    private final DictionaryTypeRepository typeRepository;
    private final DictionaryItemRepository itemRepository;
    private final DictionaryService dictionaryService;

    @Transactional
    public Map<String, Object> importFromExcel(MultipartFile file) throws IOException {
        Map<String, Object> result = new HashMap<>();
        int typeCount = 0;
        int itemCount = 0;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            // Sheet1: 字典分类
            Sheet typeSheet = workbook.getSheet("DictionaryTypes");
            if (typeSheet != null) {
                Map<String, Long> typeCodeToId = new HashMap<>();

                for (int i = 1; i <= typeSheet.getLastRowNum(); i++) {
                    Row row = typeSheet.getRow(i);
                    if (row == null) continue;

                    String code = getCellValue(row.getCell(0));
                    String name = getCellValue(row.getCell(1));
                    String parentCode = getCellValue(row.getCell(2));
                    String description = getCellValue(row.getCell(3));
                    Integer sortOrder = parseInt(getCellValue(row.getCell(4)));
                    Boolean enabled = parseBoolean(getCellValue(row.getCell(5)));

                    if (code == null || name == null) continue;

                    Long parentId = parentCode != null ? 
                            typeRepository.findByCodeAndIsDeletedFalse(parentCode)
                                    .map(DictionaryType::getId).orElse(null) : null;

                    DictionaryType type = typeRepository.findByCodeAndIsDeletedFalse(code).orElse(null);
                    if (type == null) {
                        type = new DictionaryType();
                        type.setCode(code.toUpperCase());
                        type.setParentId(parentId);
                        type.setName(name);
                        type.setDescription(description);
                        type.setSortOrder(sortOrder != null ? sortOrder : 0);
                        type.setEnabled(enabled != null ? enabled : true);
                        type = typeRepository.save(type);
                        typeCount++;
                    }
                    typeCodeToId.put(code, type.getId());
                }

                // 处理其他Sheet: 枚举值
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    Sheet sheet = workbook.getSheetAt(i);
                    String sheetName = sheet.getSheetName();
                    if (!sheetName.startsWith("Items_")) continue;

                    String typeCode = sheetName.substring(6);
                    Long typeId = typeCodeToId.get(typeCode);
                    if (typeId == null) {
                        typeId = typeRepository.findByCodeAndIsDeletedFalse(typeCode)
                                .map(DictionaryType::getId).orElse(null);
                    }
                    if (typeId == null) continue;

                    for (int j = 1; j <= sheet.getLastRowNum(); j++) {
                        Row row = sheet.getRow(j);
                        if (row == null) continue;

                        String itemCode = getCellValue(row.getCell(0));
                        String itemName = getCellValue(row.getCell(1));
                        String value = getCellValue(row.getCell(2));
                        String itemDesc = getCellValue(row.getCell(3));
                        Integer sortOrder = parseInt(getCellValue(row.getCell(4)));
                        Boolean enabled = parseBoolean(getCellValue(row.getCell(5)));
                        Boolean isDefault = parseBoolean(getCellValue(row.getCell(6)));
                        String color = getCellValue(row.getCell(7));

                        if (itemCode == null || itemName == null) continue;

                        if (!itemRepository.existsByTypeIdAndCodeAndIsDeletedFalse(typeId, itemCode)) {
                            DictionaryItem item = new DictionaryItem();
                            item.setTypeId(typeId);
                            item.setCode(itemCode);
                            item.setName(itemName);
                            item.setValue(value);
                            item.setDescription(itemDesc);
                            item.setSortOrder(sortOrder != null ? sortOrder : 0);
                            item.setEnabled(enabled != null ? enabled : true);
                            item.setIsDefault(isDefault != null ? isDefault : false);
                            item.setColor(color);
                            itemRepository.save(item);
                            itemCount++;
                        }
                    }
                }
            }
        }

        result.put("typeCount", typeCount);
        result.put("itemCount", itemCount);
        return result;
    }

    public byte[] exportToExcel() throws IOException {
        List<DictionaryType> types = typeRepository.findByIsDeletedFalseOrderBySortOrderAsc();

        try (Workbook workbook = new XSSFWorkbook()) {
            // Sheet1: 字典分类
            Sheet typeSheet = workbook.createSheet("DictionaryTypes");
            createTypeHeader(typeSheet);

            int rowNum = 1;
            for (DictionaryType type : types) {
                Row row = typeSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(type.getCode());
                row.createCell(1).setCellValue(type.getName());
                if (type.getParentId() != null) {
                    typeRepository.findByIdAndIsDeletedFalse(type.getParentId())
                            .ifPresent(parent -> row.createCell(2).setCellValue(parent.getCode()));
                }
                row.createCell(3).setCellValue(type.getDescription());
                row.createCell(4).setCellValue(type.getSortOrder());
                row.createCell(5).setCellValue(type.getEnabled() != null && type.getEnabled() ? "启用" : "禁用");
            }

            // 每个分类一个Sheet
            for (DictionaryType type : types) {
                String sheetName = "Items_" + type.getCode();
                Sheet itemSheet = workbook.createSheet(sheetName);
                createItemHeader(itemSheet);

                List<DictionaryItem> items = itemRepository.findByTypeIdAndIsDeletedFalseOrderBySortOrderAsc(type.getId());
                int itemRowNum = 1;
                for (DictionaryItem item : items) {
                    Row row = itemSheet.createRow(itemRowNum++);
                    row.createCell(0).setCellValue(item.getCode());
                    row.createCell(1).setCellValue(item.getName());
                    row.createCell(2).setCellValue(item.getValue());
                    row.createCell(3).setCellValue(item.getDescription());
                    row.createCell(4).setCellValue(item.getSortOrder());
                    row.createCell(5).setCellValue(item.getEnabled() != null && item.getEnabled() ? "启用" : "禁用");
                    row.createCell(6).setCellValue(item.getIsDefault() != null && item.getIsDefault() ? "是" : "否");
                    row.createCell(7).setCellValue(item.getColor());
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private void createTypeHeader(Sheet sheet) {
        Row header = sheet.createRow(0);
        String[] headers = {"分类编码", "分类名称", "父分类编码", "描述", "排序", "启用状态"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
        }
    }

    private void createItemHeader(Sheet sheet) {
        Row header = sheet.createRow(0);
        String[] headers = {"枚举编码", "枚举名称", "实际值", "描述", "排序", "启用状态", "是否默认", "颜色"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private Integer parseInt(String value) {
        if (value == null) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean parseBoolean(String value) {
        if (value == null) return null;
        return "启用".equals(value) || "是".equals(value) || "true".equalsIgnoreCase(value);
    }
}
```

**Step 2: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/service/DictionaryExcelService.java
git commit -m "feat: add dictionary Excel import/export service"
```

---

## Task 9: 创建Controller

**Files:**
- Create: `backend/src/main/java/com/frosts/testplatform/controller/DictionaryController.java`

**Step 1: 实现Controller**

```java
package com.frosts.testplatform.controller;

import com.frosts.testplatform.common.ApiResponse;
import com.frosts.testplatform.dto.dictionary.*;
import com.frosts.testplatform.service.DictionaryExcelService;
import com.frosts.testplatform.service.DictionaryPermissionService;
import com.frosts.testplatform.service.DictionaryService;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<ApiResponse<Page<DictionaryItemResponse>>> getItems(
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
    public ResponseEntity<ApiResponse<Page<DictionaryLogResponse>>> getLogs(
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
```

**Step 2: Commit**

```bash
git add backend/src/main/java/com/frosts/testplatform/controller/DictionaryController.java
git commit -m "feat: add dictionary controller with CRUD, import/export and logging"
```

---

## Task 10: 创建前端API模块

**Files:**
- Create: `frontend/src/api/dictionary.ts`

**Step 1: 实现前端API**

```typescript
import request from '@/utils/request'

export interface DictionaryType {
  id: number
  parentId?: number
  code: string
  name: string
  description?: string
  sortOrder: number
  enabled: boolean
  isSystem?: boolean
  children?: DictionaryType[]
  createdAt?: string
  updatedAt?: string
}

export interface DictionaryItem {
  id: number
  typeId: number
  typeCode?: string
  typeName?: string
  code: string
  name: string
  value?: string
  description?: string
  sortOrder: number
  enabled: boolean
  isDefault?: boolean
  color?: string
  createdAt?: string
  updatedAt?: string
}

export interface DictionaryLog {
  id: number
  typeId?: number
  typeCode?: string
  itemId?: number
  itemCode?: string
  action: string
  oldValue?: string
  newValue?: string
  operator: string
  operatedAt: string
  ipAddress?: string
}

export interface CreateTypeRequest {
  parentId?: number
  code: string
  name: string
  description?: string
  sortOrder?: number
  enabled?: boolean
}

export interface UpdateTypeRequest {
  parentId?: number
  code: string
  name: string
  description?: string
  sortOrder?: number
  enabled?: boolean
}

export interface CreateItemRequest {
  typeId: number
  code: string
  name: string
  value?: string
  description?: string
  sortOrder?: number
  enabled?: boolean
  isDefault?: boolean
  color?: string
}

export interface UpdateItemRequest {
  code: string
  name: string
  value?: string
  description?: string
  sortOrder?: number
  enabled?: boolean
  isDefault?: boolean
  color?: string
}

export interface TypePermission {
  roleId: number
  permission: 'READ' | 'WRITE' | 'ADMIN'
}

// 分类管理
export const getDictionaryTypeTree = () => {
  return request.get('/dictionary/types') as Promise<ApiResponse<DictionaryType[]>>
}

export const getDictionaryType = (id: number) => {
  return request.get(`/dictionary/types/${id}`) as Promise<ApiResponse<DictionaryType>>
}

export const createDictionaryType = (data: CreateTypeRequest) => {
  return request.post('/dictionary/types', data) as Promise<ApiResponse<DictionaryType>>
}

export const updateDictionaryType = (id: number, data: UpdateTypeRequest) => {
  return request.put(`/dictionary/types/${id}`, data) as Promise<ApiResponse<DictionaryType>>
}

export const deleteDictionaryType = (id: number) => {
  return request.delete(`/dictionary/types/${id}`) as Promise<ApiResponse<null>>
}

// 枚举值管理
export const getDictionaryItems = (typeId: number, params?: { keyword?: string; page?: number; size?: number }) => {
  return request.get(`/dictionary/types/${typeId}/items`, { params }) as Promise<ApiResponse<DictionaryItem[]>>
}

export const getDictionaryItemsByCode = (typeCode: string) => {
  return request.get(`/dictionary/types/code/${typeCode}/items`) as Promise<ApiResponse<DictionaryItem[]>>
}

export const createDictionaryItem = (data: CreateItemRequest) => {
  return request.post('/dictionary/items', data) as Promise<ApiResponse<DictionaryItem>>
}

export const updateDictionaryItem = (id: number, data: UpdateItemRequest) => {
  return request.put(`/dictionary/items/${id}`, data) as Promise<ApiResponse<DictionaryItem>>
}

export const deleteDictionaryItem = (id: number) => {
  return request.delete(`/dictionary/items/${id}`) as Promise<ApiResponse<null>>
}

export const updateDictionaryItemStatus = (id: number, enabled: boolean) => {
  return request.patch(`/dictionary/items/${id}/status?enabled=${enabled}`) as Promise<ApiResponse<DictionaryItem>>
}

// 权限管理
export const getTypePermissions = (id: number) => {
  return request.get(`/dictionary/types/${id}/permissions`) as Promise<ApiResponse<Array<{ roleId: number; permission: string }>>>
}

export const setTypePermissions = (id: number, permissions: TypePermission[]) => {
  return request.put(`/dictionary/types/${id}/permissions`, { permissions }) as Promise<ApiResponse<null>>
}

// 导入导出
export const importDictionaryExcel = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/dictionary/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }) as Promise<ApiResponse<{ typeCount: number; itemCount: number }>>
}

export const exportDictionaryExcel = () => {
  return request.get('/dictionary/export', { responseType: 'blob' }) as Promise<Blob>
}

// 操作日志
export const getDictionaryLogs = (params?: {
  typeId?: number
  operator?: string
  startTime?: string
  endTime?: string
  page?: number
  size?: number
}) => {
  return request.get('/dictionary/logs', { params }) as Promise<ApiResponse<DictionaryLog[]>>
}
```

**Step 2: Commit**

```bash
git add frontend/src/api/dictionary.ts
git commit -m "feat: add dictionary frontend API module"
```

---

## Task 11: 创建前端页面组件

**Files:**
- Create: `frontend/src/pages/DictionaryManagement.tsx`

**Step 1: 实现主页面组件**

```tsx
import React, { useCallback, useEffect, useRef, useState } from 'react'
import {
  Button,
  Card,
  Col,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Switch,
  Tag,
  Tree,
  Upload,
  Drawer,
  Descriptions,
  Empty,
  message,
  Tabs,
  Table,
} from 'antd'
import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  UploadOutlined,
  DownloadOutlined,
  HistoryOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons'
import type { DataNode } from 'antd/es/tree'
import { ProTable, type ActionType, type ProColumns } from '@ant-design/pro-components'
import {
  getDictionaryTypeTree,
  createDictionaryType,
  updateDictionaryType,
  deleteDictionaryType,
  getDictionaryItems,
  createDictionaryItem,
  updateDictionaryItem,
  deleteDictionaryItem,
  updateDictionaryItemStatus,
  importDictionaryExcel,
  exportDictionaryExcel,
  getDictionaryLogs,
  getTypePermissions,
  setTypePermissions,
  type DictionaryType,
  type DictionaryItem,
  type DictionaryLog,
} from '../api/dictionary'
import { getSystemRoles, type Role } from '../api/system'
import useMessage from '../hooks/useMessage'

interface TypeFormValues {
  parentId?: number
  code: string
  name: string
  description?: string
  sortOrder?: number
  enabled?: boolean
}

interface ItemFormValues {
  code: string
  name: string
  value?: string
  description?: string
  sortOrder?: number
  enabled?: boolean
  isDefault?: boolean
  color?: string
}

const COLORS = [
  '#1890ff', '#52c41a', '#faad14', '#f5222d', '#722ed1',
  '#13c2c2', '#eb2f96', '#fa541c', '#fa8c16', '#a0d911',
]

const toTreeData = (types: DictionaryType[]): DataNode[] =>
  types.map((type) => ({
    key: type.id,
    title: `${type.name} (${type.code})`,
    children: type.children ? toTreeData(type.children) : undefined,
  }))

const findTypeById = (types: DictionaryType[], id: number): DictionaryType | undefined => {
  for (const type of types) {
    if (type.id === id) return type
    if (type.children) {
      const found = findTypeById(type.children, id)
      if (found) return found
    }
  }
  return undefined
}

const flattenTypes = (types: DictionaryType[]): DictionaryType[] =>
  types.flatMap((type) => [type, ...(type.children ? flattenTypes(type.children) : [])])

const DictionaryManagement: React.FC = () => {
  const msg = useMessage()
  const [types, setTypes] = useState<DictionaryType[]>([])
  const [selectedTypeId, setSelectedTypeId] = useState<number>()
  const [roles, setRoles] = useState<Role[]>([])
  const [typeModalVisible, setTypeModalVisible] = useState(false)
  const [itemModalVisible, setItemModalVisible] = useState(false)
  const [permissionModalVisible, setPermissionModalVisible] = useState(false)
  const [logDrawerVisible, setLogDrawerVisible] = useState(false)
  const [editingType, setEditingType] = useState<DictionaryType | null>(null)
  const [editingItem, setEditingItem] = useState<DictionaryItem | null>(null)
  const [logs, setLogs] = useState<DictionaryLog[]>([])
  const [typeForm] = Form.useForm<TypeFormValues>()
  const [itemForm] = Form.useForm<ItemFormValues>()
  const [permissionForm] = Form.useForm<{ permissions: Array<{ roleId: number; permission: string }> }>()
  const itemActionRef = useRef<ActionType>()

  const selectedType = selectedTypeId ? findTypeById(types, selectedTypeId) : undefined
  const allTypes = flattenTypes(types)

  const loadTypes = useCallback(async () => {
    const response = await getDictionaryTypeTree()
    if (response.code === 200) {
      setTypes(response.data || [])
    }
  }, [])

  const loadRoles = useCallback(async () => {
    const response = await getSystemRoles({ page: 0, size: 200 })
    if (response.code === 200) {
      setRoles(response.data || [])
    }
  }, [])

  useEffect(() => {
    loadTypes()
    loadRoles()
  }, [loadTypes, loadRoles])

  // 分类操作
  const openCreateType = (parent?: DictionaryType) => {
    setEditingType(null)
    typeForm.resetFields()
    typeForm.setFieldsValue({ parentId: parent?.id, enabled: true, sortOrder: 0 })
    setTypeModalVisible(true)
  }

  const openEditType = (type: DictionaryType) => {
    setEditingType(type)
    typeForm.setFieldsValue({
      parentId: type.parentId,
      code: type.code,
      name: type.name,
      description: type.description,
      sortOrder: type.sortOrder,
      enabled: type.enabled,
    })
    setTypeModalVisible(true)
  }

  const submitType = async () => {
    const values = await typeForm.validateFields()
    try {
      if (editingType) {
        await updateDictionaryType(editingType.id, values)
        msg.success('分类更新成功')
      } else {
        await createDictionaryType(values)
        msg.success('分类创建成功')
      }
      setTypeModalVisible(false)
      await loadTypes()
    } catch (error: any) {
      msg.error(error.response?.data?.message || (editingType ? '分类更新失败' : '分类创建失败'))
    }
  }

  const removeType = async (id: number) => {
    try {
      await deleteDictionaryType(id)
      msg.success('分类删除成功')
      setSelectedTypeId(undefined)
      await loadTypes()
    } catch (error: any) {
      msg.error(error.response?.data?.message || '分类删除失败')
    }
  }

  // 枚举值操作
  const openCreateItem = () => {
    setEditingItem(null)
    itemForm.resetFields()
    itemForm.setFieldsValue({ enabled: true, sortOrder: 0, isDefault: false })
    setItemModalVisible(true)
  }

  const openEditItem = (item: DictionaryItem) => {
    setEditingItem(item)
    itemForm.setFieldsValue({
      code: item.code,
      name: item.name,
      value: item.value,
      description: item.description,
      sortOrder: item.sortOrder,
      enabled: item.enabled,
      isDefault: item.isDefault,
      color: item.color,
    })
    setItemModalVisible(true)
  }

  const submitItem = async () => {
    if (!selectedTypeId) return
    const values = await itemForm.validateFields()
    try {
      if (editingItem) {
        await updateDictionaryItem(editingItem.id, values)
        msg.success('枚举值更新成功')
      } else {
        await createDictionaryItem({ ...values, typeId: selectedTypeId })
        msg.success('枚举值创建成功')
      }
      setItemModalVisible(false)
      itemActionRef.current?.reload()
    } catch (error: any) {
      msg.error(error.response?.data?.message || (editingItem ? '枚举值更新失败' : '枚举值创建失败'))
    }
  }

  const removeItem = async (id: number) => {
    try {
      await deleteDictionaryItem(id)
      msg.success('枚举值删除成功')
      itemActionRef.current?.reload()
    } catch (error: any) {
      msg.error(error.response?.data?.message || '枚举值删除失败')
    }
  }

  const toggleItemStatus = async (item: DictionaryItem) => {
    try {
      await updateDictionaryItemStatus(item.id, !item.enabled)
      msg.success(item.enabled ? '枚举值已禁用' : '枚举值已启用')
      itemActionRef.current?.reload()
    } catch (error: any) {
      msg.error('状态更新失败')
    }
  }

  // 权限设置
  const openPermissionModal = async () => {
    if (!selectedTypeId) return
    const response = await getTypePermissions(selectedTypeId)
    if (response.code === 200) {
      permissionForm.setFieldsValue({ permissions: response.data || [] })
      setPermissionModalVisible(true)
    }
  }

  const submitPermissions = async () => {
    if (!selectedTypeId) return
    const values = await permissionForm.validateFields()
    try {
      await setTypePermissions(selectedTypeId, values.permissions || [])
      msg.success('权限设置成功')
      setPermissionModalVisible(false)
    } catch (error: any) {
      msg.error('权限设置失败')
    }
  }

  // 导入导出
  const handleImport = async (file: File) => {
    try {
      const response = await importDictionaryExcel(file)
      if (response.code === 200) {
        msg.success(`导入成功: ${response.data?.typeCount || 0} 个分类, ${response.data?.itemCount || 0} 个枚举值`)
        await loadTypes()
      } else {
        msg.error(response.message || '导入失败')
      }
    } catch (error: any) {
      msg.error('导入失败')
    }
    return false
  }

  const handleExport = async () => {
    try {
      const blob = await exportDictionaryExcel()
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `dictionary_export_${new Date().toISOString().slice(0, 10)}.xlsx`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
      msg.success('导出成功')
    } catch (error) {
      msg.error('导出失败')
    }
  }

  // 日志
  const openLogDrawer = async () => {
    const response = await getDictionaryLogs({ page: 0, size: 50 })
    if (response.code === 200) {
      setLogs(response.data || [])
      setLogDrawerVisible(true)
    }
  }

  const itemColumns: ProColumns<DictionaryItem>[] = [
    {
      title: '关键词',
      dataIndex: 'keyword',
      hideInTable: true,
      fieldProps: { placeholder: '编码 / 名称' },
    },
    {
      title: '枚举编码',
      dataIndex: 'code',
      search: false,
      width: 120,
    },
    {
      title: '枚举名称',
      dataIndex: 'name',
      search: false,
      width: 120,
    },
    {
      title: '实际值',
      dataIndex: 'value',
      search: false,
      width: 120,
      render: (_: any, record: DictionaryItem) => record.value || '-',
    },
    {
      title: '描述',
      dataIndex: 'description',
      search: false,
      ellipsis: true,
      render: (_: any, record: DictionaryItem) => record.description || '-',
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      search: false,
      width: 80,
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      search: false,
      width: 100,
      render: (_, record) => (
        <Tag color={record.enabled ? 'green' : 'default'}>
          {record.enabled ? '启用' : '禁用'}
        </Tag>
      ),
    },
    {
      title: '默认',
      dataIndex: 'isDefault',
      search: false,
      width: 80,
      render: (_, record) => record.isDefault ? <Tag color="blue">是</Tag> : <Tag>否</Tag>,
    },
    {
      title: '颜色',
      dataIndex: 'color',
      search: false,
      width: 80,
      render: (_, record) => record.color ? (
        <div style={{ width: 20, height: 20, backgroundColor: record.color, borderRadius: 4 }} />
      ) : '-',
    },
    {
      title: '操作',
      valueType: 'option',
      width: 200,
      fixed: 'right',
      render: (_, record) => (
        <Space size={16}>
          <Button type="link" style={{ padding: 0 }} icon={<EditOutlined />} onClick={() => openEditItem(record)}>
            编辑
          </Button>
          <Button type="link" style={{ padding: 0 }} onClick={() => toggleItemStatus(record)}>
            {record.enabled ? '禁用' : '启用'}
          </Button>
          <Popconfirm title="确定删除此枚举值吗？" onConfirm={() => removeItem(record.id)}>
            <Button type="link" style={{ padding: 0 }} icon={<DeleteOutlined />} danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  const logColumns = [
    { title: '操作类型', dataIndex: 'action', width: 100 },
    { title: '分类ID', dataIndex: 'typeId', width: 80 },
    { title: '项ID', dataIndex: 'itemId', width: 80 },
    { title: '操作人', dataIndex: 'operator', width: 100 },
    { title: '操作时间', dataIndex: 'operatedAt', width: 180 },
    { title: 'IP地址', dataIndex: 'ipAddress', width: 120 },
  ]

  return (
    <div>
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col span={24}>
          <Space>
            <Upload beforeUpload={handleImport} showUploadList={false}>
              <Button icon={<UploadOutlined />}>导入Excel</Button>
            </Upload>
            <Button icon={<DownloadOutlined />} onClick={handleExport}>导出Excel</Button>
            <Button icon={<HistoryOutlined />} onClick={openLogDrawer}>操作日志</Button>
          </Space>
        </Col>
      </Row>

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={8}>
          <Card
            title="字典分类"
            extra={
              <Button type="primary" icon={<PlusOutlined />} onClick={() => openCreateType()}>
                新建分类
              </Button>
            }
          >
            {types.length > 0 ? (
              <Tree
                blockNode
                defaultExpandAll
                selectedKeys={selectedTypeId ? [selectedTypeId] : []}
                treeData={toTreeData(types)}
                onSelect={(keys) => {
                  if (keys[0]) {
                    setSelectedTypeId(Number(keys[0]))
                  }
                }}
                titleRender={(node) => (
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <span>{node.title as string}</span>
                    <Space size={4}>
                      <Button
                        type="link"
                        size="small"
                        icon={<PlusOutlined />}
                        onClick={(e) => {
                          e.stopPropagation()
                          const type = findTypeById(types, node.key as number)
                          if (type) openCreateType(type)
                        }}
                      />
                      <Button
                        type="link"
                        size="small"
                        icon={<EditOutlined />}
                        onClick={(e) => {
                          e.stopPropagation()
                          const type = findTypeById(types, node.key as number)
                          if (type) openEditType(type)
                        }}
                      />
                      <Popconfirm
                        title="确定删除此分类吗？"
                        onConfirm={(e) => {
                          e?.stopPropagation()
                          removeType(node.key as number)
                        }}
                      >
                        <Button type="link" size="small" icon={<DeleteOutlined />} danger />
                      </Popconfirm>
                    </Space>
                  </div>
                )}
              />
            ) : (
              <Empty description="暂无字典分类" />
            )}
          </Card>
        </Col>

        <Col xs={24} lg={16}>
          <Card
            title={selectedType ? `${selectedType.name} (${selectedType.code})` : '请选择字典分类'}
            extra={
              selectedType ? (
                <Space>
                  <Button icon={<SafetyCertificateOutlined />} onClick={openPermissionModal}>
                    权限设置
                  </Button>
                  <Button type="primary" icon={<PlusOutlined />} onClick={openCreateItem}>
                    新建枚举值
                  </Button>
                </Space>
              ) : null
            }
          >
            {selectedType ? (
              <ProTable<DictionaryItem>
                columns={itemColumns}
                actionRef={itemActionRef}
                rowKey="id"
                search={false}
                headerTitle={`${selectedType.name} - 枚举值列表`}
                request={async (params) => {
                  const response = await getDictionaryItems(selectedType.id, {
                    keyword: params.keyword as string | undefined,
                    page: (params.current || 1) - 1,
                    size: params.pageSize || 10,
                  })
                  return {
                    data: response.data || [],
                    success: response.code === 200,
                    total: response.total || 0,
                  }
                }}
                pagination={{
                  pageSize: 10,
                  showSizeChanger: true,
                }}
              />
            ) : (
              <Empty description="请选择左侧的字典分类" />
            )}
          </Card>
        </Col>
      </Row>

      {/* 分类编辑弹窗 */}
      <Modal
        title={editingType ? '编辑分类' : '新建分类'}
        open={typeModalVisible}
        onOk={submitType}
        onCancel={() => setTypeModalVisible(false)}
      >
        <Form form={typeForm} layout="vertical">
          <Form.Item name="parentId" label="父分类">
            <Select
              allowClear
              placeholder="不选择则作为根分类"
              options={allTypes
                .filter((t) => !editingType || t.id !== editingType.id)
                .map((t) => ({ label: `${t.name} (${t.code})`, value: t.id }))}
            />
          </Form.Item>
          <Form.Item
            name="code"
            label="分类编码"
            rules={[{ required: true, message: '请输入分类编码' }]}
          >
            <Input placeholder="例如 DEFECT_STATUS" disabled={Boolean(editingType?.isSystem)} />
          </Form.Item>
          <Form.Item name="name" label="分类名称" rules={[{ required: true, message: '请输入分类名称' }]}>
            <Input placeholder="请输入分类名称" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} placeholder="请输入描述" />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="enabled" label="状态" valuePropName="checked">
            <Switch checkedChildren="启用" unCheckedChildren="禁用" />
          </Form.Item>
        </Form>
      </Modal>

      {/* 枚举值编辑弹窗 */}
      <Modal
        title={editingItem ? '编辑枚举值' : '新建枚举值'}
        open={itemModalVisible}
        onOk={submitItem}
        onCancel={() => setItemModalVisible(false)}
      >
        <Form form={itemForm} layout="vertical">
          <Form.Item name="code" label="枚举编码" rules={[{ required: true, message: '请输入枚举编码' }]}>
            <Input placeholder="例如 HIGH" />
          </Form.Item>
          <Form.Item name="name" label="枚举名称" rules={[{ required: true, message: '请输入枚举名称' }]}>
            <Input placeholder="请输入枚举名称" />
          </Form.Item>
          <Form.Item name="value" label="实际值">
            <Input placeholder="请输入实际值（可选）" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={2} placeholder="请输入描述" />
          </Form.Item>
          <Form.Item name="color" label="颜色">
            <Select placeholder="请选择颜色" allowClear>
              {COLORS.map((color) => (
                <Select.Option key={color} value={color}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <div style={{ width: 16, height: 16, backgroundColor: color, borderRadius: 4 }} />
                    {color}
                  </div>
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="sortOrder" label="排序">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="enabled" label="启用状态" valuePropName="checked">
                <Switch checkedChildren="启用" unCheckedChildren="禁用" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="isDefault" label="是否默认" valuePropName="checked">
                <Switch checkedChildren="是" unCheckedChildren="否" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>

      {/* 权限设置弹窗 */}
      <Modal
        title="权限设置"
        open={permissionModalVisible}
        onOk={submitPermissions}
        onCancel={() => setPermissionModalVisible(false)}
      >
        <Form form={permissionForm} layout="vertical">
          <Form.List name="permissions">
            {(fields, { add, remove }) => (
              <>
                {fields.map((field) => (
                  <Row key={field.key} gutter={8} align="middle">
                    <Col span={10}>
                      <Form.Item
                        {...field}
                        name={[field.name, 'roleId']}
                        rules={[{ required: true, message: '请选择角色' }]}
                      >
                        <Select placeholder="选择角色" options={roles.map((r) => ({ label: r.name, value: r.id }))} />
                      </Form.Item>
                    </Col>
                    <Col span={10}>
                      <Form.Item
                        {...field}
                        name={[field.name, 'permission']}
                        rules={[{ required: true, message: '请选择权限' }]}
                      >
                        <Select
                          placeholder="选择权限"
                          options={[
                            { label: '只读', value: 'READ' },
                            { label: '读写', value: 'WRITE' },
                            { label: '管理', value: 'ADMIN' },
                          ]}
                        />
                      </Form.Item>
                    </Col>
                    <Col span={4}>
                      <Button type="link" danger onClick={() => remove(field.name)}>
                        删除
                      </Button>
                    </Col>
                  </Row>
                ))}
                <Button type="dashed" onClick={() => add()} block>
                  添加权限
                </Button>
              </>
            )}
          </Form.List>
        </Form>
      </Modal>

      {/* 操作日志抽屉 */}
      <Drawer
        title="操作日志"
        width={800}
        open={logDrawerVisible}
        onClose={() => setLogDrawerVisible(false)}
      >
        <Table
          dataSource={logs}
          columns={logColumns}
          rowKey="id"
          pagination={{ pageSize: 20 }}
          size="small"
        />
      </Drawer>
    </div>
  )
}

export default DictionaryManagement
```

**Step 2: Commit**

```bash
git add frontend/src/pages/DictionaryManagement.tsx
git commit -m "feat: add dictionary management frontend page"
```

---

## Task 12: 添加路由和菜单

**Files:**
- Modify: `frontend/src/App.tsx`

**Step 1: 添加路由**

```tsx
import DictionaryManagement from './pages/DictionaryManagement'

// 在路由配置中添加
<Route path="/dictionary" element={<DictionaryManagement />} />
```

**Step 2: 在系统管理页面添加入口（或添加到主导航）**

修改 `frontend/src/components/Layout.tsx` 或 `frontend/src/App.tsx` 添加数据字典管理菜单项。

**Step 3: Commit**

```bash
git add frontend/src/App.tsx
git commit -m "feat: add dictionary management route"
```

---

## Task 13: 编译和测试

**Step 1: 后端编译**

```bash
cd backend
mvn clean compile
```

**Step 2: 前端编译**

```bash
cd frontend
pnpm install
pnpm run build
```

**Step 3: 运行测试**

```bash
cd backend
mvn test
```

**Step 4: Commit**

```bash
git commit -m "test: verify dictionary management module compilation"
```

---

## 总结

本实施计划包含13个任务，涵盖：

1. **数据库设计**: 4张表（分类、枚举值、权限、日志）
2. **后端实现**: 实体、Repository、DTO、权限服务、业务服务、Excel服务、Controller
3. **前端实现**: API模块、管理页面（树形分类、枚举值表格、权限设置、日志查看）
4. **功能完整**: CRUD、启用/禁用、Excel导入导出、Redis缓存、操作日志、权限控制

每个任务都包含具体的文件路径、代码实现和提交命令，确保实施过程清晰可控。
