package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.application.dto.response.DatabaseResetResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.test.database-reset.enabled", havingValue = "true")
public class DatabaseResetService {
    private static final String TABLES_SQL = """
            SELECT name
            FROM sqlite_master
            WHERE type = 'table'
              AND name NOT LIKE 'sqlite_%'
            ORDER BY name
            """;

    private final DataSource dataSource;

    public synchronized DatabaseResetResponse reset() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(true);
            try {
                setForeignKeys(connection, false);
                connection.setAutoCommit(false);
                try {
                    List<String> tables = tableNames(connection);
                    long rowsDeleted = clearTables(connection, tables);
                    resetSequences(connection);
                    connection.commit();
                    return new DatabaseResetResponse(tables.size(), rowsDeleted, LocalDateTime.now());
                } catch (SQLException exception) {
                    connection.rollback();
                    throw exception;
                }
            } finally {
                connection.setAutoCommit(true);
                setForeignKeys(connection, true);
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }

    private List<String> tableNames(Connection connection) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(TABLES_SQL)) {
            while (resultSet.next()) {
                tables.add(resultSet.getString("name"));
            }
        }
        return tables;
    }

    private long clearTables(Connection connection, List<String> tables) throws SQLException {
        long rowsDeleted = 0;
        try (Statement statement = connection.createStatement()) {
            for (String table : tables) {
                rowsDeleted += statement.executeUpdate("DELETE FROM " + quote(table));
            }
        }
        return rowsDeleted;
    }

    private void resetSequences(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM sqlite_sequence");
        } catch (SQLException exception) {
            if (exception.getMessage() == null || !exception.getMessage().contains("no such table")) {
                throw exception;
            }
        }
    }

    private void setForeignKeys(Connection connection, boolean enabled) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = " + (enabled ? "ON" : "OFF"));
        }
    }

    private String quote(String identifier) {
        return '"' + identifier.replace("\"", "\"\"") + '"';
    }
}
