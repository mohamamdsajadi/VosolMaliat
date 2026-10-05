package com.mparsict.vosolmaliat.unit.config;

import com.mparsict.vosolmaliat.common.config.SystemActorInitializer;
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
            statement.execute("PRAGMA foreign_keys = ON");
            SystemActorInitializer.seed(connection);
            statement.executeUpdate("""
                    INSERT INTO paper_type
                        (paper_type, paper_title, created_at, updated_at, created_by, updated_by,
                         creator_user_id, updater_user_id)
                    VALUES ('TEST', 'نوع آزمایشی', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
                            'SYSTEM', 'SYSTEM', 0, 0)
                    """);
            try (var result = statement.executeQuery("PRAGMA integrity_check")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isEqualTo("ok");
            }
            try (var result = statement.executeQuery("PRAGMA foreign_key_check")) {
                assertThat(result.next()).isFalse();
            }
            assertThat(tableNames(connection)).contains(
                    // Tables defined by VEM - مدل داده 14050707.
                    "final_tax_debt_paper",
                    "execution_paper",
                    "execution_paper_notification",
                    "debt_information",
                    "taxpayer",
                    "taxpayer_activity_type",
                    "natural_person",
                    "legal_person",
                    "natural_person_extra_info",
                    "legal_person_extra_info",
                    "contact_info",
                    "related_persons",
                    "restrictions_applied_to_companies",
                    "dynamic_control_rule",
                    "execution_paper_state_history",
                    "debt_information_history",
                    "dynamic_control_rule_history",
                    "taxpayer_activity_type_history",
                    "natural_person_extra_info_history",
                    "legal_person_extra_info_history",
                    "contact_info_history",
                    "execution_paper_issuance_request",
                    "debt_inquiry_result",
                    "general_directorate_of_tax_affairs",
                    "tax_affairs_office",
                    "activity_type",
                    "paper_type",
                    // Existing application-support tables intentionally preserved.
                    "app_user_profile",
                    "permission",
                    "group_permission",
                    "audit_log",
                    "execution_number_sequence",
                    "general_directorate_of_tax_affairs_history",
                    "tax_affairs_office_history",
                    "activity_type_history",
                    "execution_workflow_state",
                    "related_person_post",
                    "debt_paper_push_record",
                    "sanim_audit_entry",
                    "fake_electronic_notification_request",
                    "fake_official_gazette_request",
                    "fake_restriction_request",
                    "fake_sanim_debt_inquiry_request",
                    "fake_sanim_inform_request"
            );
            assertThat(tableNames(connection)).doesNotContain(
                    "debt_detection_document",
                    "finalized_debt_paper",
                    "execution_paper_rejection_rule",
                    "execution_paper_inform_rejection_rule",
                    "execution_paper_rejection_rule_history",
                    "execution_paper_inform_rejection_rule_history"
            );
            assertThat(columnNames(connection, "final_tax_debt_paper")).containsExactlyInAnyOrder(
                    "id", "paper_type", "finalization_reason", "unique_final_tax_debt_notice_id",
                    "final_tax_debt_notice_issue_date", "tax_return_number", "tax_return_date",
                    "final_tax_debt_paper_notification_number",
                    "final_tax_debt_paper_notification_view_date", "taxpayer_id", "national_code",
                    "national_id", "tax_source", "tax_year", "tax_period", "period_start_date",
                    "period_end_date", "tax_affairs_office_id", "created_at", "updated_at",
                    "created_by", "updated_by", "creator_user_id", "updater_user_id"
            );
            assertThat(columnNames(connection, "debt_information")).contains(
                    "debt_paper_unique_id", "execution_paper_id", "total_liability",
                    "total_penalties", "total_credits", "total_payments", "total_remissions",
                    "outstanding_tax_debt", "penalties_detail", "credits_detail",
                    "payments_detail", "remissions_detail"
            );
            assertThat(columnNames(connection, "dynamic_control_rule")).contains(
                    "parent_rule_id", "rule_scope", "json", "limitation_start_date",
                    "limitation_end_date", "document_no", "document_file_path", "error_code",
                    "error_message", "is_active", "creation_date", "update_date"
            );
            assertThat(columnNames(connection, "execution_paper_state_history")).contains(
                    "execution_paper_id", "state", "creation_date", "user_id"
            );
            assertThat(columnNames(connection, "execution_paper")).containsExactlyInAnyOrder(
                    "issuance_date", "execution_paper_unique_code", "final_tax_debt_paper_id",
                    "old_execution_letter_number", "old_execution_letter_date", "id",
                    "created_at", "updated_at", "created_by", "updated_by",
                    "creator_user_id", "updater_user_id"
            );
            assertThat(columnNames(connection, "execution_paper_notification")).contains(
                    "notification_date", "execution_paper_id", "electronic_notification_id",
                    "taxpayer_notification_view_date", "update_date"
            );
            assertThat(columnNames(connection, "taxpayer")).contains(
                    "registeration_trace_code", "taxpayer_id", "natural_person_id",
                    "legal_person_id", "taxpayer_type", "creation_date"
            );
            assertThat(columnNames(connection, "taxpayer_activity_type")).contains(
                    "taxpayer_id", "activity_type_id", "creation_date", "update_date"
            );
            assertThat(columnNames(connection, "natural_person")).contains("national_code", "fida", "creation_date");
            assertThat(columnNames(connection, "legal_person")).contains("national_id", "creation_date");
            assertThat(columnNames(connection, "natural_person_extra_info")).contains(
                    "natural_person_id", "first_name", "last_name", "full_name", "father_name",
                    "birth_date", "is_foreign", "creation_date", "update_date"
            );
            assertThat(columnNames(connection, "legal_person_extra_info")).contains(
                    "legal_person_id", "registration_number", "registration_date", "name", "trade_name"
            );
            assertThat(columnNames(connection, "contact_info")).contains(
                    "natural_person_id", "legal_person_id", "phone_no", "mobile_no", "zipcode",
                    "address", "email"
            );
            assertThat(columnNames(connection, "related_persons")).contains(
                    "taxpayer_id", "related_natural_person_id", "related_legal_person_id",
                    "natural_person_related_to_legal_person_id", "post_name", "start_date", "end_date",
                    "position_expiry_date", "is_board_member", "is_partner", "has_signatory_authority",
                    "creation_date"
            );
            assertThat(columnNames(connection, "restrictions_applied_to_companies")).contains(
                    "finalized_debt_paper_notification_id", "natural_person_id", "legal_person_id", "result",
                    "creation_date"
            );
            assertThat(columnNames(connection, "debt_information_history")).contains(
                    "debt_paper_unique_id", "execution_paper_id", "total_liability", "total_penalties",
                    "total_credits", "total_payments", "total_remissions", "outstanding_tax_debt",
                    "penalties_detail", "credits_detail", "payments_detail", "remissions_detail",
                    "next_inquiry_date", "creation_date"
            );
            assertThat(columnNames(connection, "dynamic_control_rule_history")).contains(
                    "parent_rule_id", "rule_scope", "json", "limitation_start_date",
                    "limitation_end_date", "document_no", "document_file_path", "description",
                    "error_code", "error_message", "is_active", "history_creation_date",
                    "next_version_creation_date", "creation_date", "update_date"
            );
            assertThat(columnNames(connection, "taxpayer_activity_type_history")).contains(
                    "taxpayer_id", "activity_type_id", "next_version_creation_date",
                    "creation_date", "update_date"
            );
            assertThat(columnNames(connection, "natural_person_extra_info_history")).contains(
                    "natural_person_id", "first_name", "last_name", "full_name", "father_name",
                    "birth_date", "is_foreign", "next_version_creation_date", "creation_date", "update_date"
            );
            assertThat(columnNames(connection, "legal_person_extra_info_history")).contains(
                    "legal_person_id", "registration_number", "registration_date", "name", "trade_name",
                    "next_version_creation_date"
            );
            assertThat(columnNames(connection, "contact_info_history")).contains(
                    "natural_person_id", "legal_person_id", "phone_no", "mobile_no", "zipcode",
                    "address", "email", "next_version_creation_date"
            );
            assertThat(columnNames(connection, "execution_paper_issuance_request")).contains(
                    "debt_paper_unique_code", "xml", "hash", "rejection_reason",
                    "execution_paper_issuance_request_rejection_reason_id"
            );
            assertThat(columnNames(connection, "debt_inquiry_result")).contains(
                    "finalized_debt_paper_notification_id", "xml", "hash"
            );
            assertThat(columnNames(connection, "general_directorate_of_tax_affairs")).contains(
                    "general_directorate_of_tax_affairs_name", "zipcode", "address", "phone"
            );
            assertThat(columnNames(connection, "tax_affairs_office")).contains(
                    "general_directorate_of_tax_affairs_id", "office_name", "zipcode", "address", "phone"
            );
            assertThat(columnNames(connection, "activity_type")).contains("inta_code", "title");
            assertThat(columnNames(connection, "paper_type")).contains("paper_type", "paper_title");
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

    private java.util.Set<String> columnNames(java.sql.Connection connection, String table) throws Exception {
        java.util.Set<String> names = new java.util.HashSet<>();
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery("PRAGMA table_info(\"" + table.replace("\"", "\"\"") + "\")")) {
            while (rows.next()) names.add(rows.getString("name"));
        }
        return names;
    }
}
