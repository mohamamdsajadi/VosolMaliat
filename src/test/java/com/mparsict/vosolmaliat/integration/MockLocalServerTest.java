package com.mparsict.vosolmaliat.integration;

import com.mparsict.vosolmaliat.application.service.cartable.CartableOperations;
import com.mparsict.vosolmaliat.application.service.cartable.OfflineCartableService;
import com.mparsict.vosolmaliat.common.config.security.KeycloakAdminBootstrapService;
import io.camunda.client.CamundaClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:sqlite:file:mock-local-integration?mode=memory&cache=shared",
        "logging.level.com.mparsict.vosolmaliat=INFO"
})
@ActiveProfiles("mock-local")
class MockLocalServerTest {
    @Autowired TestRestTemplate http;
    @Autowired CartableOperations cartable;
    @Autowired CamundaClient camunda;
    @Autowired KeycloakAdminBootstrapService keycloakBootstrap;
    @Autowired com.mparsict.vosolmaliat.common.config.security.SecurityDataInitializer securityData;
    @Autowired com.mparsict.vosolmaliat.domain.repository.security.GroupPermissionRepository grants;

    @Test
    void startsOfflineAndSupportsLoginPermissionsAndRevokedLogout() {
        assertThat(cartable).isInstanceOf(OfflineCartableService.class);
        assertThat(keycloakBootstrap.isEnabled()).isFalse();
        assertThatThrownBy(() -> camunda.newCreateInstanceCommand())
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        var config = http.getForEntity("/api/v1/auth/config", Map.class);
        assertThat(config.getBody()).containsEntry("provider", "mock").containsEntry("workflow", "mock");
        assertThat(config.getHeaders().getCacheControl()).contains("no-store");
        assertThat(http.getForEntity("/api/v1/auth/me", String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        var users = http.getForEntity("/api/v1/auth/mock/users", String.class);
        assertThat(users.getBody()).contains("demo_tax_directorate_director_general");
        var login = http.postForEntity("/api/v1/auth/mock/login", Map.of("username", "demo_tax_directorate_director_general"), Map.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = login.getBody().get("accessToken").toString();
        var headers = new HttpHeaders();
        headers.setBearerAuth(token);
        var request = new HttpEntity<>(headers);
        var me = http.exchange("/api/v1/auth/me", HttpMethod.GET, request, Map.class);
        assertThat(me.getBody()).containsEntry("businessRole", "TAX_DIRECTORATE_DIRECTOR_GENERAL");
        assertThat(http.exchange("/api/v1/cartable/my-tasks", HttpMethod.GET, request, String.class).getBody()).isEqualTo("[]");
        assertThat(http.exchange("/api/v1/cartable/tasks/1", HttpMethod.GET, request, String.class).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(http.exchange("/api/v1/security-bootstrap/keycloak-sync-status", HttpMethod.GET, request, String.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(http.getForEntity("/api/v1/dashboard/stats", String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(http.exchange("/api/v1/dashboard/stats", HttpMethod.GET, request, String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(http.exchange("/api/v1/auth/mock/logout", HttpMethod.POST, request, String.class).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(http.exchange("/api/v1/auth/me", HttpMethod.GET, request, String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void demoHasOneAccountPerRoleAndRevocationsSurviveBootstrap() {
        var users = http.getForEntity("/api/v1/auth/mock/users", java.util.List.class).getBody();
        assertThat(users).hasSize(11);
        assertThat(users.toString()).doesNotContain("tehran_", "isfahan_", "tax_expert");
        for (var role : com.mparsict.vosolmaliat.domain.enums.BusinessRole.values()) {
            String username = role.name().equals("ADMIN") ? "admin" : "demo_" + role.name().toLowerCase(java.util.Locale.ROOT);
            assertThat(users.toString()).contains(username);
        }
        var login = http.postForEntity("/api/v1/auth/mock/login", Map.of("username", "admin"), Map.class);
        var headers = new HttpHeaders(); headers.setBearerAuth(login.getBody().get("accessToken").toString());
        String role = "EXECUTION_OFFICER";
        var grant = grants.findByGroupNameAndPermissionCode(role, "EXECUTION_PAPER_LIST_VIEW").orElseThrow();
        assertThat(http.exchange("/api/v1/group-permissions/" + grant.getId(), HttpMethod.DELETE, new HttpEntity<>(headers), String.class).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        securityData.initializeSecurityData();
        assertThat(grants.findByGroupNameAndPermissionCode(role, "EXECUTION_PAPER_LIST_VIEW")).isEmpty();
        var officerLogin = http.postForEntity("/api/v1/auth/mock/login", Map.of("username", "demo_execution_officer"), Map.class);
        var officerHeaders = new HttpHeaders(); officerHeaders.setBearerAuth(officerLogin.getBody().get("accessToken").toString());
        assertThat(http.exchange("/api/v1/execution-papers", HttpMethod.GET, new HttpEntity<>(officerHeaders), String.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(http.exchange("/api/v1/group-permissions", HttpMethod.POST, new HttpEntity<>(Map.of("groupName", role, "permissionCode", "EXECUTION_PAPER_LIST_VIEW"), headers), Map.class).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(http.exchange("/api/v1/execution-papers", HttpMethod.GET, new HttpEntity<>(officerHeaders), String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        var adminGrant = grants.findByGroupNameAndPermissionCode("ADMIN", "GROUP_PERMISSION_UPDATE").orElseThrow();
        assertThat(http.exchange("/api/v1/group-permissions/" + adminGrant.getId(), HttpMethod.DELETE, new HttpEntity<>(headers), String.class).getStatusCode().is4xxClientError()).isTrue();
        assertThat(grants.findById(adminGrant.getId())).isPresent();
    }
}
