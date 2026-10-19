package com.mparsict.vosolmaliat.common.config.security;

import com.mparsict.vosolmaliat.domain.model.security.AppUserProfile;
import com.mparsict.vosolmaliat.domain.repository.security.AppUserProfileRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Local-only opaque sessions; never accepts unsigned JWTs or caller-supplied roles. */
@Service
@ConditionalOnProperty(name = "app.security.provider", havingValue = "mock")
public class MockIdentityProvider implements JwtDecoder {
    private static final Duration LIFETIME = Duration.ofHours(8);
    private final AppUserProfileRepository profiles;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public MockIdentityProvider(AppUserProfileRepository profiles) {
        this(profiles, Clock.systemUTC());
    }

    MockIdentityProvider(AppUserProfileRepository profiles, Clock clock) {
        this.profiles = profiles;
        this.clock = clock;
    }

    public List<MockUser> users() {
        return profiles.findAll().stream().filter(this::available)
                .sorted(java.util.Comparator.comparing(AppUserProfile::getUsername))
                .map(profile -> new MockUser(profile.getUsername(),
                        (profile.getFirstName() == null ? "" : profile.getFirstName()) + " "
                                + (profile.getLastName() == null ? "" : profile.getLastName()),
                        profile.getBusinessRole() == null ? null : profile.getBusinessRole().name()))
                .toList();
    }

    public Token login(String username) {
        AppUserProfile profile = profiles.findByUsername(username == null ? "" : username)
                .filter(this::available)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Mock user is unavailable"));
        Instant now = clock.instant();
        sessions.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = "mock_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, new Session(profile.getKeycloakUserId(), now, now.plus(LIFETIME)));
        return new Token(token, LIFETIME.toSeconds());
    }

    public void logout(String token) {
        sessions.remove(token);
    }

    @Override
    public Jwt decode(String token) {
        Session session = sessions.get(token);
        if (session == null || !session.expiresAt().isAfter(clock.instant())) {
            sessions.remove(token);
            throw new BadJwtException("Mock session is invalid or expired");
        }
        AppUserProfile profile = profiles.findByKeycloakUserId(session.subject()).filter(this::available)
                .orElseThrow(() -> new BadJwtException("Mock user is unavailable"));
        List<String> roles = profile.getBusinessRole() == null ? List.of() : List.of(profile.getBusinessRole().name());
        return Jwt.withTokenValue(token).header("typ", "mock-session")
                .subject(session.subject()).issuedAt(session.issuedAt()).expiresAt(session.expiresAt())
                .claim("preferred_username", profile.getUsername())
                .claim("realm_access", Map.of("roles", roles)).build();
    }

    private boolean available(AppUserProfile profile) {
        return Boolean.TRUE.equals(profile.getEnabledInBusinessSystem())
                && !Boolean.TRUE.equals(profile.getLockedInBusinessSystem());
    }

    private record Session(String subject, Instant issuedAt, Instant expiresAt) {}
    public record MockUser(String username, String fullName, String businessRole) {}
    public record Token(String accessToken, long expiresIn) {}
}
