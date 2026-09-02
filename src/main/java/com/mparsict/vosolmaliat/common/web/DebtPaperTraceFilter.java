package com.mparsict.vosolmaliat.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DebtPaperTraceFilter extends OncePerRequestFilter {

    public static final String ENDPOINT = "/api/v1/debt-papers/push";
    public static final String TRACE_REQUEST_ATTRIBUTE = DebtPaperTraceFilter.class.getName() + ".traceId";
    public static final String TRACE_MDC_KEY = "apiGatewayTraceID";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !ENDPOINT.equals(request.getRequestURI().substring(request.getContextPath().length()));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString();
        request.setAttribute(TRACE_REQUEST_ATTRIBUTE, traceId);
        response.setHeader("X-Trace-ID", traceId);
        MDC.put(TRACE_MDC_KEY, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_MDC_KEY);
        }
    }

    public static String traceId(HttpServletRequest request) {
        Object value = request.getAttribute(TRACE_REQUEST_ATTRIBUTE);
        return value == null ? UUID.randomUUID().toString() : value.toString();
    }
}
