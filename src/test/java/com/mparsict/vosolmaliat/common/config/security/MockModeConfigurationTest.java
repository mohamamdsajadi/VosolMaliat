package com.mparsict.vosolmaliat.common.config.security;

import com.mparsict.vosolmaliat.domain.repository.security.AppUserProfileRepository;
import com.mparsict.vosolmaliat.presentation.api.security.MockAuthController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MockModeConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Dependencies.class, MockIdentityProvider.class, MockAuthController.class);

    @Test
    void mockProviderAndRoutesAreAbsentByDefaultAndInRealMode() {
        runner.run(context -> assertThat(context).doesNotHaveBean(MockIdentityProvider.class).doesNotHaveBean(MockAuthController.class));
        runner.withPropertyValues("app.security.provider=keycloak")
                .run(context -> assertThat(context).doesNotHaveBean(MockIdentityProvider.class).doesNotHaveBean(MockAuthController.class));
    }

    @Test
    void mockProviderRequiresExplicitServerConfiguration() {
        runner.withPropertyValues("app.security.provider=mock")
                .run(context -> assertThat(context).hasSingleBean(MockIdentityProvider.class).hasSingleBean(MockAuthController.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class Dependencies {
        @Bean
        AppUserProfileRepository profiles() { return mock(AppUserProfileRepository.class); }
    }
}
