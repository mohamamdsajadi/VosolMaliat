package com.mparsict.vosolmaliat.common.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.response.ApiGatewayResponse;
import com.mparsict.vosolmaliat.application.dto.response.DebtPaperPushData;
import com.mparsict.vosolmaliat.common.web.DebtPaperTraceFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@RequiredArgsConstructor
public class DebtPaperAccessIdFilter extends OncePerRequestFilter {

    private final DebtPaperSecurityProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !DebtPaperTraceFilter.ENDPOINT.equals(
                request.getRequestURI().substring(request.getContextPath().length())
        );
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }
        String supplied = request.getHeader("AccessID");
        if (!allowed(supplied)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), ApiGatewayResponse.failure(
                    DebtPaperPushData.empty(),
                    DebtPaperTraceFilter.traceId(request),
                    false,
                    -2,
                    "AccessID is missing or unauthorized."
            ));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean allowed(String supplied) {
        if (supplied == null || supplied.isBlank()) {
            return false;
        }
        byte[] suppliedBytes = supplied.getBytes(StandardCharsets.UTF_8);
        return properties.getAccessIds().stream()
                .filter(value -> value != null && !value.isBlank())
                .anyMatch(value -> MessageDigest.isEqual(
                        suppliedBytes,
                        value.getBytes(StandardCharsets.UTF_8)
                ));
    }
}
