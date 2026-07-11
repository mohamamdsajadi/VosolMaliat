package com.mparsict.vosolmaliat.presentation.api.security;

import com.mparsict.vosolmaliat.application.dto.response.security.CurrentUserResponse;
import com.mparsict.vosolmaliat.application.service.security.SecurityIdentityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SecurityIdentityService securityIdentityService;

    @GetMapping("/me")
    public CurrentUserResponse me() {
        log.debug("GET /api/v1/auth/me");
        return securityIdentityService.currentUserResponse();
    }

    @GetMapping("/me/permissions")
    public Set<String> permissions() {
        log.debug("GET /api/v1/auth/me/permissions");
        return securityIdentityService.currentUser().permissions();
    }
}
