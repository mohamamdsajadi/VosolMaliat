package com.mparsict.vosolmaliat.presentation.api.security;

import com.mparsict.vosolmaliat.common.config.security.MockIdentityProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth/mock")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.security.provider", havingValue = "mock")
public class MockAuthController {
    private final MockIdentityProvider provider;

    @GetMapping("/users")
    public List<MockIdentityProvider.MockUser> users() {
        return provider.users();
    }

    @PostMapping("/login")
    public MockIdentityProvider.Token login(@Valid @RequestBody LoginRequest request) {
        return provider.login(request.username());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal Jwt jwt) {
        provider.logout(jwt.getTokenValue());
    }

    public record LoginRequest(@NotBlank String username) {}
}
