package com.mparsict.vosolmaliat.unit.config;

import com.mparsict.vosolmaliat.common.config.SqliteLegacySchemaMigrator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.*;
import static org.assertj.core.api.Assertions.*;

class SanimSchemaMigrationTest {
    @TempDir Path directory;
    @Test void migrationPreservesLegacyRowsAndAddsNullableExactAmountsIdempotently() throws Exception {
        String url = "jdbc:sqlite:" + directory.resolve("legacy.db");
        try (var c = DriverManager.getConnection(url); var s = c.createStatement()) {
            s.execute("CREATE TABLE finalized_debt_paper (id INTEGER PRIMARY KEY, total_finalized_amount DOUBLE)");
            s.execute("INSERT INTO finalized_debt_paper VALUES (1, 123.5)");
            s.execute("CREATE TABLE debt_information (id INTEGER PRIMARY KEY, tax_amount DOUBLE)");
            s.execute("INSERT INTO debt_information VALUES (1, 200)");
            s.execute("CREATE TABLE debt_paper_push_record (id INTEGER PRIMARY KEY, xml_payload TEXT, tax_office_id INTEGER)");
            s.execute("INSERT INTO debt_paper_push_record VALUES (1, '<original/>', 7)");
        }
        SqliteLegacySchemaMigrator.migrate(url);
        SqliteLegacySchemaMigrator.migrate(url);
        try (var files = java.nio.file.Files.list(directory)) {
            var backups = files.filter(p -> p.toString().contains("pre-sanim-cleanup")).toList();
            assertThat(backups).hasSize(1);
            try (var original = DriverManager.getConnection("jdbc:sqlite:" + backups.getFirst()); var query = original.createStatement();
                 var saved = query.executeQuery("SELECT total_finalized_amount FROM finalized_debt_paper")) {
                assertThat(saved.next()).isTrue();
                assertThat(saved.getDouble(1)).isEqualTo(123.5);
            }
        }
        try (var c = DriverManager.getConnection(url); var s = c.createStatement()) {
            try (var rows = s.executeQuery("SELECT * FROM finalized_debt_paper")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString("source_validation_status")).isEqualTo("LEGACY_REVIEW_REQUIRED");
                assertThat(rows.getString("aggregate_total_tax_amount")).isNull();
            }
            try (var rows = s.executeQuery("SELECT * FROM debt_information")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getLong("version")).isZero();
                assertThat(rows.getString("aggregate_outstanding_debt_amount")).isNull();
            }
            try (var rows = s.executeQuery("SELECT * FROM debt_paper_push_record")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString("xml_payload")).isEqualTo("<original/>");
                assertThat(rows.getString("source_tax_office_code")).isEqualTo("7");
            }
        }
    }
}
