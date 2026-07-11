package com.mparsict.vosolmaliat.application.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakPrincipalMapperTest {

    private final KeycloakPrincipalMapper mapper = new KeycloakPrincipalMapper();

    @Test
    void extractsRealmClientRolesAndNormalizedGroups() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("keycloak-id-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("preferred_username", "ali")
                .claim("realm_access", Map.of("roles", List.of("ADMIN")))
                .claim("resource_access", Map.of(
                        "vosol-api", Map.of("roles", List.of("TAX_UNIT_USER"))
                ))
                .claim("groups", List.of("/tax/Tehran", "TAX_UNIT_USER"))
                .build();

        assertThat(mapper.username(jwt)).isEqualTo("ali");
        assertThat(mapper.extractRealmRoles(jwt)).containsExactly("ADMIN");
        assertThat(mapper.extractClientRoles(jwt)).containsExactly("TAX_UNIT_USER");
        assertThat(mapper.extractGroups(jwt)).containsExactly("TAX_UNIT_USER", "Tehran");
        assertThat(mapper.extractIdentityGroups(jwt)).contains("ADMIN", "TAX_UNIT_USER", "Tehran");
    }
}
