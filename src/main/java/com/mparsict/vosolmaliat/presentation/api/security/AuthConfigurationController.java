package com.mparsict.vosolmaliat.presentation.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Public runtime mode discovery: never exposes credentials or allows mode changes. */
@RestController
public class AuthConfigurationController {
    private final String provider;
    private final boolean camundaEnabled;

    public AuthConfigurationController(@Value("${app.security.provider:keycloak}") String provider,
                                       @Value("${camunda.client.enabled:true}") boolean camundaEnabled) {
        if (!provider.equals("keycloak") && !provider.equals("mock")) {
            throw new IllegalArgumentException("app.security.provider must be keycloak or mock");
        }
        this.provider = provider;
        this.camundaEnabled = camundaEnabled;
    }

    @GetMapping("/api/v1/auth/config")
    public ResponseEntity<Map<String, String>> configuration() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(Map.of("provider", provider, "workflow", camundaEnabled ? "camunda" : "mock"));
    }
}
