package com.mparsict.vosolmaliat.presentation.api.security;

import com.mparsict.vosolmaliat.application.dto.request.security.AssignGroupPermissionRequest;
import com.mparsict.vosolmaliat.application.dto.request.security.CreatePermissionRequest;
import com.mparsict.vosolmaliat.application.dto.response.security.GroupPermissionResponse;
import com.mparsict.vosolmaliat.application.dto.response.security.PermissionResponse;
import com.mparsict.vosolmaliat.application.service.security.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping("/api/v1/permissions")
    @PreAuthorize("hasAuthority('PERMISSION_VIEW')")
    public List<PermissionResponse> listPermissions() {
        log.debug("GET /api/v1/permissions");
        return permissionService.listPermissions();
    }

    @PostMapping("/api/v1/permissions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PERMISSION_ASSIGN')")
    public PermissionResponse createPermission(@RequestBody @Valid CreatePermissionRequest request) {
        log.debug("POST /api/v1/permissions");
        return permissionService.createPermission(request);
    }

    @GetMapping("/api/v1/group-permissions")
    @PreAuthorize("hasAuthority('GROUP_PERMISSION_VIEW')")
    public List<GroupPermissionResponse> listGroupPermissions(@RequestParam(required = false) String groupName) {
        log.debug("GET /api/v1/group-permissions groupName={}", groupName);
        return permissionService.listGroupPermissions(groupName);
    }

    @PostMapping("/api/v1/group-permissions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('GROUP_PERMISSION_UPDATE')")
    public GroupPermissionResponse assignGroupPermission(@RequestBody @Valid AssignGroupPermissionRequest request) {
        log.debug("POST /api/v1/group-permissions");
        return permissionService.assignPermission(request);
    }

    @DeleteMapping("/api/v1/group-permissions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('GROUP_PERMISSION_UPDATE')")
    public void removeGroupPermission(@PathVariable Long id) {
        log.debug("DELETE /api/v1/group-permissions/{}", id);
        permissionService.removePermission(id);
    }
}
