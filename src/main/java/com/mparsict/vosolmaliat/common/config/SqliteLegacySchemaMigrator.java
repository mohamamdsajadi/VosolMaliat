package com.mparsict.vosolmaliat.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Applies compatibility changes that Hibernate cannot express through SQLite's
 * limited {@code ALTER TABLE} support. This runs before the entity manager and
 * Camunda workers are created, so workers never observe a partially upgraded
 * schema.
 */
@Slf4j
public class SqliteLegacySchemaMigrator
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String SQLITE_PREFIX = "jdbc:sqlite:";
    private static final String DEBT_INQUIRY_RESULT = "debt_inquiry_result";

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        String jdbcUrl = applicationContext.getEnvironment().getProperty("spring.datasource.url");
        if (jdbcUrl == null || !jdbcUrl.startsWith(SQLITE_PREFIX)) {
            return;
        }

        migrate(jdbcUrl);
    }

    public static void migrate(String jdbcUrl) {
        try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
            if (!tableExists(connection, DEBT_INQUIRY_RESULT)) {
                return;
            }

            boolean migrated = false;
            migrated |= addColumnIfMissing(connection, "request_id", "VARCHAR(255)");
            migrated |= addColumnIfMissing(connection, "execution_paper_id", "INTEGER");
            migrated |= addColumnIfMissing(connection, "received_at", "TIMESTAMP");

            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        CREATE UNIQUE INDEX IF NOT EXISTS uk_debt_inquiry_result_request_id
                        ON debt_inquiry_result(request_id)
                        """);
                statement.execute("""
                        CREATE INDEX IF NOT EXISTS idx_debt_inquiry_execution_paper
                        ON debt_inquiry_result(execution_paper_id)
                        """);
            }

            if (migrated) {
                log.info("Upgraded legacy debt_inquiry_result table for SANIM debt inquiry correlation");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not upgrade the SQLite debt inquiry schema", exception);
        }
    }

    private static boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static boolean addColumnIfMissing(Connection connection, String column, String type)
            throws SQLException {
        if (columnExists(connection, column)) {
            return false;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + DEBT_INQUIRY_RESULT
                    + " ADD COLUMN " + column + " " + type);
        }
        return true;
    }

    private static boolean columnExists(Connection connection, String column) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + DEBT_INQUIRY_RESULT + ")")) {
            while (resultSet.next()) {
                if (column.equalsIgnoreCase(resultSet.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }
}
