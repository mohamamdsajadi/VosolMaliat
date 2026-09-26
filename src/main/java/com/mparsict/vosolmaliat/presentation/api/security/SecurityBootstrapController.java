package com.mparsict.vosolmaliat.presentation.api.security;

import com.mparsict.vosolmaliat.common.config.security.KeycloakAdminBootstrapService;
import com.mparsict.vosolmaliat.common.config.security.SecurityDataInitializer;
import com.mparsict.vosolmaliat.domain.model.security.AppUserProfile;
import com.mparsict.vosolmaliat.domain.repository.security.AppUserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/security-bootstrap")
@RequiredArgsConstructor
public class SecurityBootstrapController {

    private final SecurityDataInitializer securityDataInitializer;
    private final KeycloakAdminBootstrapService keycloakAdminBootstrapService;
    private final AppUserProfileRepository userProfileRepository;

    @GetMapping("/keycloak-sync-status")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Map<String, Object> keycloakSyncStatus() {
        return Map.of(
                "enabled", keycloakAdminBootstrapService.isEnabled(),
                "message", keycloakAdminBootstrapService.diagnostics()
        );
    }

    @PostMapping("/sync-sample-users")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Map<String, String> syncSampleUsers() {
        securityDataInitializer.initializeSecurityData();
        return Map.of("status", "sample users and security data synchronized");
    }

    @PostMapping("/sync-keycloak-users")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional
    public Map<String, Object> syncKeycloakUsers() {
        List<Map<String, Object>> results = new ArrayList<>();
        for (AppUserProfile profile : userProfileRepository.findAll()) {
            KeycloakAdminBootstrapService.KeycloakSyncResult result = keycloakAdminBootstrapService.syncUser(
                    profile.getUsername(),
                    profile.getEmail(),
                    profile.getFirstName(),
                    profile.getLastName(),
                    profile.getBusinessRole()
            );
            if (result.keycloakUserId() != null && !result.keycloakUserId().equals(profile.getKeycloakUserId())) {
                profile.setKeycloakUserId(result.keycloakUserId());
                userProfileRepository.save(profile);
            }
            results.add(Map.of(
                    "username", profile.getUsername(),
                    "status", result.status(),
                    "keycloakUserId", result.keycloakUserId() == null ? "" : result.keycloakUserId(),
                    "message", result.message() == null ? "" : result.message()
            ));
        }
        return Map.of(
                "enabled", keycloakAdminBootstrapService.isEnabled(),
                "message", keycloakAdminBootstrapService.diagnostics(),
                "results", results
        );
    }
}
