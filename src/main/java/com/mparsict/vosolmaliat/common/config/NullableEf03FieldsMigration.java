package com.mparsict.vosolmaliat.common.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Relaxes fields that EF03 legitimately omits; SQLite cannot alter these constraints in place. */
final class NullableEf03FieldsMigration {
    private NullableEf03FieldsMigration() {}

    private static final Map<String, List<String>> NULLABLE_COLUMNS = new LinkedHashMap<>();
    static {
        NULLABLE_COLUMNS.put("natural_person_extra_info", List.of("birth_date", "father_name"));
        NULLABLE_COLUMNS.put("legal_person_extra_info", List.of(
                "registration_date", "registration_number", "trade_name"));
        NULLABLE_COLUMNS.put("contact_info", List.of("phone_no", "mobile_no"));
    }

    static void migrate(Connection connection) throws SQLException {
        boolean foreignKeys = pragma(connection, "foreign_keys") != 0;
        try (var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys=OFF");
        }
        connection.setAutoCommit(false);
        try {
            for (var entry : NULLABLE_COLUMNS.entrySet()) {
                if (requiresMigration(connection, entry.getKey(), entry.getValue())) {
                    rebuild(connection, entry.getKey(), entry.getValue());
                }
            }
            connection.commit();
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(true);
            try (var statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys=" + (foreignKeys ? "ON" : "OFF"));
            }
        }
    }

    private static boolean requiresMigration(Connection connection, String table, List<String> columns)
            throws SQLException {
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery("PRAGMA table_info(\"" + table + "\")")) {
            while (rows.next()) {
                if (columns.contains(rows.getString("name")) && rows.getInt("notnull") == 1) return true;
            }
        }
        return false;
    }

    private static void rebuild(Connection connection, String table, List<String> nullableColumns)
            throws SQLException {
        String createSql;
        try (var statement = connection.prepareStatement(
                "SELECT sql FROM sqlite_master WHERE type='table' AND name=?")) {
            statement.setString(1, table);
            try (var rows = statement.executeQuery()) {
                if (!rows.next()) return;
                createSql = rows.getString(1);
            }
        }
        String temporary = table + "__nullable_ef03";
        String migratedSql = createSql.replaceFirst(
                "(?i)CREATE\\s+TABLE\\s+\"?\\Q" + table + "\\E\"?",
                "CREATE TABLE " + temporary);
        for (String column : nullableColumns) {
            migratedSql = migratedSql.replaceFirst(
                    "(?i)(\\b\\Q" + column + "\\E\\b[^,]*?)\\s+not\\s+null",
                    "$1");
        }
        try (var statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS " + temporary);
            statement.execute(migratedSql);
            statement.execute("INSERT INTO " + temporary + " SELECT * FROM " + table);
            statement.execute("DROP TABLE " + table);
            statement.execute("ALTER TABLE " + temporary + " RENAME TO " + table);
        }
    }

    private static int pragma(Connection connection, String name) throws SQLException {
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery("PRAGMA " + name)) {
            return rows.next() ? rows.getInt(1) : 0;
        }
    }
}
