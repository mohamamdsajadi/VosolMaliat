package com.mparsict.vosolmaliat.common.config.security;

import com.mparsict.vosolmaliat.domain.model.AuditContext;
import com.mparsict.vosolmaliat.domain.model.security.AppUserProfile;
import com.mparsict.vosolmaliat.domain.repository.security.AppUserProfileRepository;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;

@Configuration @RequiredArgsConstructor
public class ModelAuditConfiguration implements WebMvcConfigurer {
    private final AppUserProfileRepository profiles;
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                AuditContext.clear();
                var auth=SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Jwt jwt) {
                    var profile=profiles.findByKeycloakUserId(jwt.getSubject()).orElseGet(() -> profiles.save(
                            AppUserProfile.builder().keycloakUserId(jwt.getSubject())
                                    .username(jwt.getClaimAsString("preferred_username") == null ? jwt.getSubject() : jwt.getClaimAsString("preferred_username"))
                                    .build()));
                    AuditContext.bind(profile.getId());
                }
                return true;
            }
            @Override public void afterCompletion(HttpServletRequest request,HttpServletResponse response,Object handler,Exception ex) {
                AuditContext.clear();
            }
        });
    }
}
