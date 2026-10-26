package com.mparsict.vosolmaliat.common.config;

import java.sql.Connection;
import java.sql.SQLException;

/** Supplies the historical issuer before Hibernate reads existing execution papers. */
final class SqliteExecutionPaperIssuerMigration {
    private SqliteExecutionPaperIssuerMigration() {}

    static void migrate(Connection connection) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name='execution_paper'");
             var tables = statement.executeQuery()) {
            if (!tables.next()) return;
        }
        boolean exists = false;
        try (var statement = connection.createStatement();
             var columns = statement.executeQuery("PRAGMA table_info(execution_paper)")) {
            while (columns.next()) {
                if ("issuer_system".equalsIgnoreCase(columns.getString("name"))) exists = true;
            }
        }
        try (var statement = connection.createStatement()) {
            if (!exists) {
                statement.execute("""
                        ALTER TABLE execution_paper ADD COLUMN issuer_system VARCHAR(16)
                        NOT NULL DEFAULT 'SANIM' CHECK (issuer_system IN ('SANIM','TABRIZ'))
                        """);
            } else {
                // A later restart must preserve an explicitly supplied TABRIZ issuer.
                statement.executeUpdate("UPDATE execution_paper SET issuer_system='SANIM' "
                        + "WHERE issuer_system IS NULL OR trim(issuer_system)=''");
            }
        }
    }
}
