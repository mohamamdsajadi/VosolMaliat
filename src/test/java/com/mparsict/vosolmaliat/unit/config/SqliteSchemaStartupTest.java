package com.mparsict.vosolmaliat.unit.config;

import jakarta.persistence.Entity;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.nio.file.Path;
import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

class SqliteSchemaStartupTest {
    @TempDir Path directory;

    @Test
    void existingApplicationSchemaCanBeUpdatedOnRepeatedStartup() throws Exception {
        var yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yaml"));
        var properties = yaml.getObject();
        assertThat(properties).isNotNull();
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        var entities = scanner.findCandidateComponents("com.mparsict.vosolmaliat.domain");
        assertThat(entities).isNotEmpty();
        for (String mode : new String[]{"create-only", "update", "update"}) {
            var configuration = new Configuration()
                    .setProperty("hibernate.connection.url", "jdbc:sqlite:" + directory.resolve("startup.db"))
                    .setProperty("hibernate.connection.driver_class", "org.sqlite.JDBC")
                    .setProperty("hibernate.dialect", properties.getProperty("spring.jpa.database-platform"))
                    .setProperty("hibernate.hbm2ddl.halt_on_error", "true");
            properties.forEach((key, value) -> {
                String name = key.toString();
                if (name.startsWith("spring.jpa.properties.")) {
                    configuration.setProperty(name.substring("spring.jpa.properties.".length()), value.toString());
                }
            });
            for (var entity : entities) {
                configuration.addAnnotatedClass(Class.forName(entity.getBeanClassName()));
            }
            configuration.setProperty("hibernate.hbm2ddl.auto", mode);
            try (var factory = configuration.buildSessionFactory()) {
                assertThat(factory.isOpen()).isTrue();
            }
        }
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("startup.db"));
             var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("PRAGMA integrity_check")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isEqualTo("ok");
            }
            assertThat(tableNames(connection)).contains(
                    "paper_type",
                    "debt_information_history",
                    "taxpayer_activity_type_history",
                    "natural_person_extra_info_history",
                    "legal_person_extra_info_history",
                    "contact_info_history",
                    "execution_paper_rejection_rule_history",
                    "execution_paper_inform_rejection_rule_history",
                    "general_directorate_of_tax_affairs_history",
                    "tax_affairs_office_history"
            );
        }
    }

    private java.util.Set<String> tableNames(java.sql.Connection connection) throws Exception {
        java.util.Set<String> names = new java.util.HashSet<>();
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table'")) {
            while (rows.next()) names.add(rows.getString(1));
        }
        return names;
    }
}
