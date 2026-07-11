package com.mparsict.vosolmaliat.common.config.security;

import com.mparsict.vosolmaliat.domain.model.security.GroupPermission;
import com.mparsict.vosolmaliat.domain.model.security.Permission;
import com.mparsict.vosolmaliat.domain.repository.security.GroupPermissionRepository;
import com.mparsict.vosolmaliat.domain.repository.security.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityDataInitializer {

    private static final List<String> PERMISSIONS = List.of(
            "USER_PROFILE_CREATE", "USER_PROFILE_UPDATE", "USER_PROFILE_VIEW", "USER_PROFILE_ENABLE_DISABLE",
            "USER_PROFILE_ASSIGN_ORGANIZATION", "PERMISSION_VIEW", "PERMISSION_ASSIGN", "GROUP_PERMISSION_VIEW",
            "GROUP_PERMISSION_UPDATE", "TASK_VIEW_OWN", "TASK_VIEW_CANDIDATE", "TASK_CLAIM", "TASK_UNCLAIM",
            "TASK_COMPLETE", "TASK_ASSIGN", "TASK_VIEW_ORG_SCOPE", "REPORT_VIEW", "AUDIT_VIEW"
    );

    private static final List<String> TAX_UNIT_USER_PERMISSIONS = List.of(
            "TASK_VIEW_OWN", "TASK_VIEW_CANDIDATE", "TASK_CLAIM", "TASK_UNCLAIM", "TASK_COMPLETE"
    );

    private final PermissionRepository permissionRepository;
    private final GroupPermissionRepository groupPermissionRepository;
    private final TransactionTemplate transactionTemplate;

    @Bean
    CommandLineRunner seedSecurityData() {
        return args -> transactionTemplate.executeWithoutResult(status -> {
            for (String code : PERMISSIONS) {
                permissionRepository.findByCode(code).orElseGet(() -> permissionRepository.save(Permission.builder()
                        .code(code)
                        .description(code)
                        .enabled(true)
                        .build()));
            }
            for (String code : PERMISSIONS) {
                ensureGroupPermission("ADMIN", code);
            }
            for (String code : TAX_UNIT_USER_PERMISSIONS) {
                ensureGroupPermission("TAX_UNIT_USER", code);
            }
        });
    }

    private void ensureGroupPermission(String groupName, String permissionCode) {
        if (groupPermissionRepository.findByGroupNameAndPermissionCode(groupName, permissionCode).isPresent()) {
            return;
        }
        Permission permission = permissionRepository.findByCode(permissionCode).orElseThrow();
        groupPermissionRepository.save(GroupPermission.builder()
                .groupName(groupName)
                .permission(permission)
                .build());
    }
}
