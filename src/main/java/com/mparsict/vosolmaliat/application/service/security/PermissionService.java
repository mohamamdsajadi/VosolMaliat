package com.mparsict.vosolmaliat.application.service.security;

import com.mparsict.vosolmaliat.application.dto.request.security.AssignGroupPermissionRequest;
import com.mparsict.vosolmaliat.application.dto.request.security.CreatePermissionRequest;
import com.mparsict.vosolmaliat.application.dto.response.security.GroupPermissionResponse;
import com.mparsict.vosolmaliat.application.dto.response.security.PermissionResponse;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.domain.model.security.GroupPermission;
import com.mparsict.vosolmaliat.domain.model.security.Permission;
import com.mparsict.vosolmaliat.domain.repository.security.GroupPermissionRepository;
import com.mparsict.vosolmaliat.domain.repository.security.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final GroupPermissionRepository groupPermissionRepository;
    private final SecurityIdentityService securityIdentityService;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissions() {
        return permissionRepository.findAll().stream().map(SecurityDtoMapper::toPermissionResponse).toList();
    }

    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        if (permissionRepository.existsByCode(request.code())) {
            throw new BusinessException("PERMISSION_ALREADY_EXISTS", "مجوز با این کد قبلا وجود دارد");
        }
        Permission saved = permissionRepository.save(Permission.builder()
                .code(request.code())
                .description(request.description())
                .enabled(true)
                .build());
        return SecurityDtoMapper.toPermissionResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<GroupPermissionResponse> listGroupPermissions(String groupName) {
        List<GroupPermission> values = groupName == null || groupName.isBlank()
                ? groupPermissionRepository.findAll()
                : groupPermissionRepository.findByGroupName(groupName);
        return values.stream().map(SecurityDtoMapper::toGroupPermissionResponse).toList();
    }

    @Transactional
    public GroupPermissionResponse assignPermission(AssignGroupPermissionRequest request) {
        try { com.mparsict.vosolmaliat.domain.enums.BusinessRole.valueOf(request.groupName()); }
        catch (IllegalArgumentException ex) { throw new BusinessException("INVALID_BUSINESS_ROLE", "نقش انتخاب‌شده معتبر نیست"); }
        Permission permission = permissionRepository.findByCode(request.permissionCode())
                .orElseThrow(() -> new BusinessException("PERMISSION_NOT_FOUND", "مجوز یافت نشد"));
        return groupPermissionRepository.findByGroupNameAndPermissionCode(request.groupName(), request.permissionCode())
                .map(SecurityDtoMapper::toGroupPermissionResponse)
                .orElseGet(() -> {
                    GroupPermission saved = groupPermissionRepository.save(GroupPermission.builder()
                            .groupName(request.groupName())
                            .permission(permission)
                            .build());
                    auditLogService.record(securityIdentityService.currentUser(), "GROUP_PERMISSION_ASSIGNED", "GROUP_PERMISSION",
                            saved.getId().toString(), null, request.groupName() + ":" + request.permissionCode());
                    return SecurityDtoMapper.toGroupPermissionResponse(saved);
                });
    }

    @Transactional
    public void removePermission(Long id) {
        GroupPermission groupPermission = groupPermissionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("GROUP_PERMISSION_NOT_FOUND", "مجوز گروه یافت نشد"));
        if ("ADMIN".equals(groupPermission.getGroupName()))
            throw new BusinessException("ADMIN_ACCESS_IMMUTABLE", "دسترسی کامل مدیر سامانه قابل لغو نیست");
        String oldValue = groupPermission.getGroupName() + ":" + groupPermission.getPermission().getCode();
        groupPermissionRepository.delete(groupPermission);
        auditLogService.record(securityIdentityService.currentUser(), "GROUP_PERMISSION_REMOVED", "GROUP_PERMISSION",
                id.toString(), oldValue, null);
    }
}
