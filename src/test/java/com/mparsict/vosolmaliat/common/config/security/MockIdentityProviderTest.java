package com.mparsict.vosolmaliat.common.config.security;

import com.mparsict.vosolmaliat.domain.enums.BusinessRole;
import com.mparsict.vosolmaliat.domain.model.security.AppUserProfile;
import com.mparsict.vosolmaliat.domain.repository.security.AppUserProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MockIdentityProviderTest {
    private final AppUserProfileRepository profiles = mock(AppUserProfileRepository.class);
    private final Instant now = Instant.parse("2026-10-19T10:00:00Z");
    private final Clock clock = mock(Clock.class);
    private final MockIdentityProvider provider = new MockIdentityProvider(profiles, clock);

    private AppUserProfile user() {
        AppUserProfile user = AppUserProfile.builder().keycloakUserId("existing-real-subject")
                .username("manager").firstName("مدیرکل").businessRole(BusinessRole.TAX_DIRECTORATE_DIRECTOR_GENERAL).build();
        when(clock.instant()).thenReturn(now);
        when(profiles.findByUsername("manager")).thenReturn(Optional.of(user));
        when(profiles.findByKeycloakUserId("existing-real-subject")).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void usesServerProfileRolesAndRevokesOnlyTheLoggedOutSession() {
        user();
        var first = provider.login("manager");
        var second = provider.login("manager");
        assertThat(first.accessToken()).startsWith("mock_").isNotEqualTo(second.accessToken());
        var jwt = provider.decode(first.accessToken());
        assertThat(jwt.getSubject()).isEqualTo("existing-real-subject");
        assertThat(jwt.getClaimAsMap("realm_access")).containsEntry("roles", List.of("TAX_DIRECTORATE_DIRECTOR_GENERAL"));
        provider.logout(first.accessToken());
        assertThatThrownBy(() -> provider.decode(first.accessToken())).isInstanceOf(BadJwtException.class);
        assertThat(provider.decode(second.accessToken()).getSubject()).isEqualTo(jwt.getSubject());
        verify(profiles, never()).save(any());
    }

    @Test
    void expiresSessionsAndRejectsUnknownOrForgedTokens() {
        user();
        var token = provider.login("manager").accessToken();
        when(clock.instant()).thenReturn(now.plusSeconds(8 * 3600));
        assertThatThrownBy(() -> provider.decode(token)).isInstanceOf(BadJwtException.class);
        assertThatThrownBy(() -> provider.decode("unsigned.jwt.token")).isInstanceOf(BadJwtException.class);
    }

    @Test
    void disabledAndLockedUsersCannotLoginAndExistingSessionsStopWorking() {
        var profile = user();
        var token = provider.login("manager").accessToken();
        profile.setLockedInBusinessSystem(true);
        assertThatThrownBy(() -> provider.login("manager")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> provider.decode(token)).isInstanceOf(BadJwtException.class);
        profile.setLockedInBusinessSystem(false);
        profile.setEnabledInBusinessSystem(false);
        when(profiles.findAll()).thenReturn(List.of(profile));
        assertThat(provider.users()).isEmpty();
        assertThatThrownBy(() -> provider.login("manager")).isInstanceOf(ResponseStatusException.class);
    }
}
