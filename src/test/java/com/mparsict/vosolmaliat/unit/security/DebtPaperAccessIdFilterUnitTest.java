package com.mparsict.vosolmaliat.unit.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.common.config.security.DebtPaperAccessIdFilter;
import com.mparsict.vosolmaliat.common.config.security.DebtPaperSecurityProperties;
import com.mparsict.vosolmaliat.common.web.DebtPaperTraceFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DebtPaperAccessIdFilterUnitTest {

    private DebtPaperAccessIdFilter filter;

    @BeforeEach
    void setUp() {
        DebtPaperSecurityProperties properties = new DebtPaperSecurityProperties();
        properties.setAccessIds(List.of("allowed-access"));
        filter = new DebtPaperAccessIdFilter(properties, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void letsUnauthenticatedRequestReachSpringSecurityAuthenticationHandling() throws Exception {
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsAuthenticatedCallerWithInvalidAccessIdUsingGatewayEnvelope() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("external", "n/a",
                        List.of(new SimpleGrantedAuthority("DEBT_PAPER_PUSH")))
        );
        MockHttpServletRequest request = request();
        request.addHeader("AccessID", "wrong-access");
        request.setAttribute(DebtPaperTraceFilter.TRACE_REQUEST_ATTRIBUTE, "trace-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString())
                .contains("\"apiGatewayTraceID\":\"trace-123\"")
                .contains("\"apiGatewayStatusCode\":-2");
    }

    @Test
    void acceptsConfiguredAccessIdForAuthenticatedCaller() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("external", "n/a",
                        List.of(new SimpleGrantedAuthority("DEBT_PAPER_PUSH")))
        );
        MockHttpServletRequest request = request();
        request.addHeader("AccessID", "allowed-access");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private MockHttpServletRequest request() {
        return new MockHttpServletRequest("POST", DebtPaperTraceFilter.ENDPOINT);
    }
}
