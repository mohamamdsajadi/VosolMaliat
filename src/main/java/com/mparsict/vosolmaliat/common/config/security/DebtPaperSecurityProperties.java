package com.mparsict.vosolmaliat.common.config.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.integration.debt-paper.security")
public class DebtPaperSecurityProperties {
    private String username;
    /** DelegatingPasswordEncoder value, for example {bcrypt}$2a$... */
    private String passwordHash;
    private List<String> accessIds = new ArrayList<>();
}
