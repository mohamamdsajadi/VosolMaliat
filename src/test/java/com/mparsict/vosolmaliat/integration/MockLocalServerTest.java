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
        assertThat(users.getBody()).contains("tehran_general_office_head");
        var login = http.postForEntity("/api/v1/auth/mock/login", Map.of("username", "tehran_general_office_head"), Map.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = login.getBody().get("accessToken").toString();
        var headers = new HttpHeaders();
        headers.setBearerAuth(token);
        var request = new HttpEntity<>(headers);
        var me = http.exchange("/api/v1/auth/me", HttpMethod.GET, request, Map.class);
        assertThat(me.getBody()).containsEntry("businessRole", "GENERAL_OFFICE_HEAD");
        assertThat(http.exchange("/api/v1/cartable/my-tasks", HttpMethod.GET, request, String.class).getBody()).isEqualTo("[]");
        assertThat(http.exchange("/api/v1/cartable/tasks/1", HttpMethod.GET, request, String.class).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(http.exchange("/api/v1/security-bootstrap/keycloak-sync-status", HttpMethod.GET, request, String.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(http.getForEntity("/api/v1/dashboard/stats", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(http.exchange("/api/v1/auth/mock/logout", HttpMethod.POST, request, String.class).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(http.exchange("/api/v1/auth/me", HttpMethod.GET, request, String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
