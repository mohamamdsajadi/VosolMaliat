package com.mparsict.vosolmaliat.common.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.mparsict.vosolmaliat.domain.repository")
@EntityScan(basePackages = "com.mparsict.vosolmaliat.domain.model")
public class JpaConfig {
    // Datasource and JPA properties are configured via application.yaml.
    // This class activates repository scanning and entity scanning for
    // the correct subpackages.
}
