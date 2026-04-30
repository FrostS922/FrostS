package com.frosts.testplatform.service;

import com.frosts.testplatform.dto.system.CreateRoleRequest;
import com.frosts.testplatform.dto.system.CreateUserRequest;
import com.frosts.testplatform.dto.system.CreateOrganizationUnitRequest;
import com.frosts.testplatform.dto.system.OrganizationUnitResponse;
import com.frosts.testplatform.dto.system.RoleResponse;
import com.frosts.testplatform.dto.system.UpdateSystemSettingRequest;
import com.frosts.testplatform.dto.system.UpdateSystemSettingsRequest;
import com.frosts.testplatform.dto.system.UserResponse;
import com.frosts.testplatform.entity.OrganizationUnit;
import com.frosts.testplatform.entity.Permission;
import com.frosts.testplatform.entity.Role;
import com.frosts.testplatform.entity.SystemSetting;
import com.frosts.testplatform.entity.User;
import com.frosts.testplatform.repository.OrganizationUnitRepository;
import com.frosts.testplatform.repository.PermissionRepository;
import com.frosts.testplatform.repository.RoleRepository;
import com.frosts.testplatform.repository.SystemSettingRepository;
import com.frosts.testplatform.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private OrganizationUnitRepository organizationUnitRepository;

    @Mock
    private SystemSettingRepository systemSettingRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SystemManagementService systemManagementService;

    @Test
    void createUserEncodesPasswordAndReturnsRolesWithoutPassword() {
        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setCode("ADMIN");
        adminRole.setName("系统管理员");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(roleRepository.findAllById(Set.of(1L))).thenReturn(List.of(adminRole));
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });

        CreateUserRequest request = new CreateUserRequest(
                " alice ",
                "secret123",
                " Alice ",
                " alice@example.com ",
                " 18800001111 ",
                true,
                Set.of(1L)
        );

        UserResponse response = systemManagementService.createUser(request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.roles()).extracting("code").containsExactly("ADMIN");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void deleteUserRejectsCurrentUser() {
        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setEnabled(true);
        admin.setIsDeleted(false);

        when(userRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> systemManagementService.deleteUser(1L, "admin"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("不能删除当前登录用户");
    }

    @Test
    void createRoleAssignsSelectedPermissions() {
        Permission readPermission = new Permission();
        readPermission.setId(1L);
        readPermission.setCode("user:read");
        readPermission.setName("查看用户");

        Permission writePermission = new Permission();
        writePermission.setId(2L);
        writePermission.setCode("user:write");
        writePermission.setName("管理用户");

        when(roleRepository.existsByCode("OPS_MANAGER")).thenReturn(false);
        when(permissionRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(readPermission, writePermission));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            role.setId(5L);
            return role;
        });

        CreateRoleRequest request = new CreateRoleRequest(
                "OPS_MANAGER",
                "运维管理员",
                "负责系统配置维护",
                Set.of(1L, 2L)
        );

        RoleResponse response = systemManagementService.createRole(request);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.code()).isEqualTo("OPS_MANAGER");
        assertThat(response.permissions()).extracting("code")
                .containsExactlyInAnyOrder("user:read", "user:write");
    }

    @Test
    void createOrganizationUnitTrimsAndLinksParent() {
        OrganizationUnit parent = new OrganizationUnit();
        parent.setId(1L);
        parent.setCode("FROSTS");
        parent.setName("FrostS");

        when(organizationUnitRepository.existsByCode("QA_CENTER")).thenReturn(false);
        when(organizationUnitRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(parent));
        when(organizationUnitRepository.save(any(OrganizationUnit.class))).thenAnswer(invocation -> {
            OrganizationUnit unit = invocation.getArgument(0);
            unit.setId(2L);
            return unit;
        });

        CreateOrganizationUnitRequest request = new CreateOrganizationUnitRequest(
                1L,
                " 质量中心 ",
                "QA_CENTER",
                "department",
                " Alice ",
                " qa@example.com ",
                " 18800002222 ",
                10,
                true,
                " 负责测试治理 "
        );

        OrganizationUnitResponse response = systemManagementService.createOrganizationUnit(request);

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.parentId()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("质量中心");
        assertThat(response.type()).isEqualTo("DEPARTMENT");

        ArgumentCaptor<OrganizationUnit> unitCaptor = ArgumentCaptor.forClass(OrganizationUnit.class);
        verify(organizationUnitRepository).save(unitCaptor.capture());
        assertThat(unitCaptor.getValue().getParent()).isEqualTo(parent);
        assertThat(unitCaptor.getValue().getLeader()).isEqualTo("Alice");
    }

    @Test
    void deleteOrganizationUnitRejectsNodeWithChildren() {
        OrganizationUnit parent = new OrganizationUnit();
        parent.setId(1L);
        parent.setCode("QA_CENTER");
        parent.setName("质量中心");
        parent.setEnabled(true);
        parent.setIsDeleted(false);

        when(organizationUnitRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(parent));
        when(organizationUnitRepository.countByParent_IdAndIsDeletedFalse(1L)).thenReturn(2L);

        assertThatThrownBy(() -> systemManagementService.deleteOrganizationUnit(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("存在下级组织，不能删除");
    }

    @Test
    void updateSystemSettingsNormalizesBooleanValue() {
        SystemSetting setting = new SystemSetting();
        setting.setId(1L);
        setting.setSettingKey("notification.email_enabled");
        setting.setSettingValue("false");
        setting.setDefaultValue("false");
        setting.setName("启用邮件通知");
        setting.setCategory("NOTIFICATION");
        setting.setValueType("BOOLEAN");
        setting.setSortOrder(10);
        setting.setEditable(true);

        when(systemSettingRepository.findBySettingKeyAndIsDeletedFalse("notification.email_enabled"))
                .thenReturn(Optional.of(setting));
        when(systemSettingRepository.save(any(SystemSetting.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(systemSettingRepository.findByIsDeletedFalseOrderByCategoryAscSortOrderAsc()).thenReturn(List.of(setting));

        systemManagementService.updateSystemSettings(new UpdateSystemSettingsRequest(List.of(
                new UpdateSystemSettingRequest("notification.email_enabled", "TRUE")
        )));

        assertThat(setting.getSettingValue()).isEqualTo("true");
    }

    @Test
    void updateSystemSettingsRejectsInvalidSelectValue() {
        SystemSetting setting = new SystemSetting();
        setting.setId(1L);
        setting.setSettingKey("quality.default_priority");
        setting.setSettingValue("MEDIUM");
        setting.setDefaultValue("MEDIUM");
        setting.setName("默认优先级");
        setting.setCategory("QUALITY");
        setting.setValueType("SELECT");
        setting.setOptions("LOW,MEDIUM,HIGH,URGENT");
        setting.setEditable(true);

        when(systemSettingRepository.findBySettingKeyAndIsDeletedFalse("quality.default_priority"))
                .thenReturn(Optional.of(setting));

        assertThatThrownBy(() -> systemManagementService.updateSystemSettings(new UpdateSystemSettingsRequest(List.of(
                new UpdateSystemSettingRequest("quality.default_priority", "BLOCKER")
        ))))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("设置值不在可选范围内: quality.default_priority");
    }
}
