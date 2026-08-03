package com.mparsict.vosolmaliat.unit.config;

import com.mparsict.vosolmaliat.common.config.SqliteLegacySchemaMigrator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SqliteLegacySchemaMigratorUnitTest {

    @TempDir
    Path tempDirectory;

    @Test
    void upgradesLegacyDebtInquiryTableAndPreservesItsRows() throws Exception {
        String jdbcUrl = "jdbc:sqlite:" + tempDirectory.resolve("legacy.sqlite");
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE debt_inquiry_result (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        finalized_debt_paper_notification_id VARCHAR(255),
                        response_json TEXT,
                        hash VARCHAR(255)
                    )
                    """);
            statement.execute("""
                    INSERT INTO debt_inquiry_result
                        (finalized_debt_paper_notification_id, response_json, hash)
                    VALUES ('legacy-1', '{}', 'hash-1')
                    """);
        }

        SqliteLegacySchemaMigrator.migrate(jdbcUrl);
        SqliteLegacySchemaMigrator.migrate(jdbcUrl);

        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement statement = connection.createStatement()) {
            assertThat(columns(statement))
                    .contains("request_id", "execution_paper_id", "received_at");
            try (ResultSet resultSet = statement.executeQuery(
                    "SELECT finalized_debt_paper_notification_id FROM debt_inquiry_result")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString(1)).isEqualTo("legacy-1");
            }
        }
    }

    private Set<String> columns(Statement statement) throws Exception {
        Set<String> columns = new HashSet<>();
        try (ResultSet resultSet = statement.executeQuery("PRAGMA table_info(debt_inquiry_result)")) {
            while (resultSet.next()) {
                columns.add(resultSet.getString("name"));
            }
        }
        return columns;
    }
}
