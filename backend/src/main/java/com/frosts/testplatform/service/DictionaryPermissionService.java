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
                .collect(java.util.stream.Collectors.toSet());

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
