package com.mparsict.vosolmaliat.common.config.security;

import com.mparsict.vosolmaliat.domain.enums.BusinessRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakAdminBootstrapService {

    private final RestClient.Builder restClientBuilder;

    @Value("${app.keycloak.bootstrap-users.enabled:false}")
    private boolean enabled;

    @Value("${app.keycloak.bootstrap-users.issuer-uri:}")
    private String issuerUri;

    @Value("${app.keycloak.bootstrap-users.client-id:}")
    private String clientId;

    @Value("${app.keycloak.bootstrap-users.client-secret:}")
    private String clientSecret;

    @Value("${app.keycloak.bootstrap-users.default-password:}")
    private String defaultPassword;

    @Value("${app.keycloak.bootstrap-users.temporary-password:true}")
    private boolean temporaryPassword;

    public boolean isEnabled() {
        return enabled;
    }

    public Optional<String> ensureUser(
            String username,
            String email,
            String firstName,
            String lastName,
            BusinessRole role) {
        KeycloakSyncResult result = syncUser(username, email, firstName, lastName, role);
        return result.keycloakUserId() == null ? Optional.empty() : Optional.of(result.keycloakUserId());
    }

    public KeycloakSyncResult syncUser(
            String username,
            String email,
            String firstName,
            String lastName,
            BusinessRole role) {
        String validationMessage = validationMessage();
        if (validationMessage != null) {
            return new KeycloakSyncResult(username, null, "DISABLED", validationMessage);
        }

        try {
            String token = adminToken();
            RestClient restClient = restClientBuilder.baseUrl(adminBaseUrl()).build();
            Optional<String> existingUserId = findUserId(restClient, token, username);
            boolean existed = existingUserId.isPresent();
            String userId = existingUserId.orElseGet(() -> createUser(restClient, token, username, email, firstName, lastName));
            if (role != null) {
                try {
                    assignRealmRole(restClient, token, userId, role.name());
                } catch (Exception ex) {
                    return new KeycloakSyncResult(username, userId, existed ? "EXISTING_ROLE_FAILED" : "CREATED_ROLE_FAILED",
                            "User synced, but realm role assignment failed: " + ex.getMessage());
                }
            }
            return new KeycloakSyncResult(username, userId, existed ? "EXISTING" : "CREATED", "User synchronized");
        } catch (Exception ex) {
            log.warn("Keycloak bootstrap user sync failed for username={}: {}", username, ex.getMessage());
            return new KeycloakSyncResult(username, null, "FAILED", ex.getMessage());
        }
    }

    public String diagnostics() {
        String validation = validationMessage();
        if (validation != null) {
            return validation;
        }
        return "Keycloak bootstrap sync is enabled for realm " + realmName();
    }

    private String validationMessage() {
        if (!enabled) {
            return "KEYCLOAK_BOOTSTRAP_USERS_ENABLED is false";
        }
        if (isBlank(issuerUri)) {
            return "KEYCLOAK_ISSUER_URI/app.keycloak.bootstrap-users.issuer-uri is empty";
        }
        if (isBlank(clientId)) {
            return "KEYCLOAK_BOOTSTRAP_CLIENT_ID is empty";
        }
        if (isBlank(clientSecret)) {
            return "KEYCLOAK_BOOTSTRAP_CLIENT_SECRET is empty";
        }
        return null;
    }

    private String adminToken() {
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        Map<?, ?> response = restClientBuilder.build()
                .post()
                .uri(tokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        Object accessToken = response == null ? null : response.get("access_token");
        if (accessToken == null) {
            throw new IllegalStateException("Keycloak admin token response did not contain access_token");
        }
        return accessToken.toString();
    }

    private Optional<String> findUserId(RestClient restClient, String token, String username) {
        List<?> users = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users")
                        .queryParam("username", username)
                        .queryParam("exact", true)
                        .build())
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(List.class);
        if (users == null || users.isEmpty() || !(users.getFirst() instanceof Map<?, ?> user)) {
            return Optional.empty();
        }
        Object id = user.get("id");
        return id == null ? Optional.empty() : Optional.of(id.toString());
    }

    private String createUser(RestClient restClient, String token, String username, String email, String firstName, String lastName) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("username", username);
        payload.put("email", email);
        payload.put("firstName", firstName);
        payload.put("lastName", lastName);
        payload.put("enabled", true);
        payload.put("emailVerified", true);
        if (!isBlank(defaultPassword)) {
            payload.put("credentials", List.of(Map.of(
                    "type", "password",
                    "value", defaultPassword,
                    "temporary", temporaryPassword
            )));
        }
        if (temporaryPassword) {
            payload.put("requiredActions", List.of("UPDATE_PASSWORD"));
        }

        restClient.post()
                .uri("/users")
                .headers(headers -> headers.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();

        return findUserId(restClient, token, username)
                .orElseThrow(() -> new IllegalStateException("Created Keycloak user but could not read generated id"));
    }

    private void assignRealmRole(RestClient restClient, String token, String userId, String roleName) {
        Map<?, ?> role = restClient.get()
                .uri("/roles/{roleName}", roleName)
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .body(Map.class);
        if (role == null) {
            return;
        }
        restClient.post()
                .uri("/users/{userId}/role-mappings/realm", userId)
                .headers(headers -> headers.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role))
                .retrieve()
                .toBodilessEntity();
    }

    private String tokenUrl() {
        return normalizedIssuerUri() + "/protocol/openid-connect/token";
    }

    private String adminBaseUrl() {
        String issuer = normalizedIssuerUri();
        String realmPath = "/realms/" + realmName();
        String base = issuer.endsWith(realmPath)
                ? issuer.substring(0, issuer.length() - realmPath.length())
                : issuer.replaceFirst("/realms/[^/]+$", "");
        return base + "/admin/realms/" + realmName();
    }

    private String realmName() {
        String path = URI.create(normalizedIssuerUri()).getPath();
        int marker = path.lastIndexOf("/realms/");
        if (marker < 0) {
            throw new IllegalStateException("Keycloak issuer-uri must contain /realms/{realm}");
        }
        return path.substring(marker + "/realms/".length());
    }

    private String normalizedIssuerUri() {
        return issuerUri.endsWith("/") ? issuerUri.substring(0, issuerUri.length() - 1) : issuerUri;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record KeycloakSyncResult(
            String username,
            String keycloakUserId,
            String status,
            String message
    ) {
    }
}
