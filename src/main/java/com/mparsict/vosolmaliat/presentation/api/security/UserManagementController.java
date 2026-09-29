package com.mparsict.vosolmaliat.presentation.api.security;

import com.mparsict.vosolmaliat.application.dto.request.security.*;
import com.mparsict.vosolmaliat.application.dto.response.security.AppUserProfileResponse;
import com.mparsict.vosolmaliat.application.service.security.AppUserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final AppUserProfileService userProfileService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('USER_PROFILE_CREATE')")
    public AppUserProfileResponse create(@RequestBody @Valid CreateAppUserProfileRequest request) {
        log.debug("POST /api/v1/users");
        return userProfileService.create(request);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_PROFILE_VIEW')")
    public List<AppUserProfileResponse> list() {
        log.debug("GET /api/v1/users");
        return userProfileService.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_PROFILE_VIEW')")
    public AppUserProfileResponse get(@PathVariable Long id) {
        log.debug("GET /api/v1/users/{}", id);
        return userProfileService.get(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_PROFILE_UPDATE')")
    public AppUserProfileResponse update(@PathVariable Long id, @RequestBody @Valid UpdateAppUserProfileRequest request) {
        log.debug("PUT /api/v1/users/{}", id);
        return userProfileService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER_PROFILE_ENABLE_DISABLE')")
    public AppUserProfileResponse status(@PathVariable Long id, @RequestBody @Valid UserStatusRequest request) {
        log.debug("PATCH /api/v1/users/{}/status", id);
        return userProfileService.updateStatus(id, request);
    }

    @PatchMapping("/{id}/lock")
    @PreAuthorize("hasAuthority('USER_PROFILE_ENABLE_DISABLE')")
    public AppUserProfileResponse lock(@PathVariable Long id, @RequestBody @Valid UserLockRequest request) {
        log.debug("PATCH /api/v1/users/{}/lock", id);
        return userProfileService.updateLock(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('USER_PROFILE_UPDATE')")
    public void delete(@PathVariable Long id) {
        log.debug("DELETE /api/v1/users/{}", id);
        userProfileService.delete(id);
    }
}
