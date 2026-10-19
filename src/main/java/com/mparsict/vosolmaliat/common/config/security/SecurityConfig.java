package com.mparsict.vosolmaliat.common.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.response.ApiGatewayResponse;
import com.mparsict.vosolmaliat.application.dto.response.DebtPaperPushData;
import com.mparsict.vosolmaliat.common.web.DebtPaperTraceFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "app.ef03.import-samples", havingValue = "false", matchIfMissing = true)
@EnableMethodSecurity
@EnableConfigurationProperties(DebtPaperSecurityProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final KeycloakJwtAuthenticationConverter jwtAuthenticationConverter;
    private final ObjectMapper objectMapper;

    @Value("${app.security.provider:keycloak}")
    private String identityProvider;

    @Bean
    AuthenticationProvider debtPaperAuthenticationProvider(DebtPaperSecurityProperties properties) {
        UserDetailsService users = username -> {
            if (properties.getUsername() == null
                    || properties.getUsername().isBlank()
                    || !properties.getUsername().equals(username)
                    || properties.getPasswordHash() == null
                    || properties.getPasswordHash().isBlank()) {
                throw new UsernameNotFoundException("Inbound Debt Paper user not found");
            }
            return User.withUsername(properties.getUsername())
                    .password(properties.getPasswordHash())
                    .authorities("DEBT_PAPER_PUSH")
                    .build();
        };
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(PasswordEncoderFactories.createDelegatingPasswordEncoder());
        return provider;
    }

    @Bean
    @Order(1)
    SecurityFilterChain debtPaperSecurityFilterChain(
            HttpSecurity http,
            AuthenticationProvider debtPaperAuthenticationProvider,
            DebtPaperAccessIdFilter accessIdFilter) throws Exception {
        return http
                .securityMatcher(DebtPaperTraceFilter.ENDPOINT)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().hasAnyAuthority(
                        "DEBT_PAPER_PUSH", "USER_PROFILE_ASSIGN_ORGANIZATION"))
                .authenticationProvider(debtPaperAuthenticationProvider)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .httpBasic(basic -> basic.authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    objectMapper.writeValue(response.getOutputStream(), ApiGatewayResponse.failure(
                            DebtPaperPushData.empty(),
                            DebtPaperTraceFilter.traceId(request),
                            false,
                            -1,
                            "Basic authentication is missing or invalid."
                    ));
                }))
                .exceptionHandling(exceptions -> exceptions.accessDeniedHandler((request, response, exception) -> {
                    response.setStatus(403);
                    response.setContentType("application/json");
                    objectMapper.writeValue(response.getOutputStream(), ApiGatewayResponse.failure(
                            DebtPaperPushData.empty(),
                            DebtPaperTraceFilter.traceId(request),
                            false,
                            -2,
                            "Caller is not authorized to push Debt Papers."
                    ));
                }))
                .addFilterAfter(accessIdFilter, BasicAuthenticationFilter.class)
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/api/v1/auth/config").permitAll();
                    if ("mock".equals(identityProvider)) {
                        auth.requestMatchers("/api/v1/auth/mock/users", "/api/v1/auth/mock/login").permitAll();
                    }
                    auth
                        .requestMatchers("/", "/index.html", "/_next/**", "/images/**", "/fonts/**",
                                "/brand/**", "/favicon.ico", "/login", "/register", "/auth/callback",
                                "/api/v1/test/registrations",
                                "/api/v1/dashboard/stats",
                                "/api/v1/fake-electronic-notification/callback").permitAll()
                        .anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }
}
