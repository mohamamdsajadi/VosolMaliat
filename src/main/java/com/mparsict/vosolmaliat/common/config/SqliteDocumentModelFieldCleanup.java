package com.mparsict.vosolmaliat.common.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Removes legacy columns from documented entities when upgrading an existing SQLite database. */
final class SqliteDocumentModelFieldCleanup {
    private SqliteDocumentModelFieldCleanup() {}

    private static final Map<String, List<String>> REMOVED_COLUMNS = new LinkedHashMap<>();
    static {
        REMOVED_COLUMNS.put("natural_person_extra_info", List.of("arrest_status", "capital_status",
                "certificate_of_no_criminal_record", "first_name_en", "last_name_en",
                "management_declaration", "passport_expiry_date"));
        REMOVED_COLUMNS.put("natural_person_extra_info_history", List.of("arrest_status", "capital_status",
                "certificate_of_no_criminal_record", "first_name_en", "last_name_en",
                "management_declaration", "passport_expiry_date"));
        REMOVED_COLUMNS.put("legal_person_extra_info", List.of("arrest_status", "capital_status",
                "management_declaration", "date_of_termination"));
        REMOVED_COLUMNS.put("legal_person_extra_info_history", List.of("arrest_status", "capital_status",
                "management_declaration", "date_of_termination"));
        REMOVED_COLUMNS.put("contact_info", List.of("fax_no", "website"));
        REMOVED_COLUMNS.put("contact_info_history", List.of("fax_no", "website"));
    }

    static void migrate(Connection connection) throws SQLException {
        for (var entry : REMOVED_COLUMNS.entrySet()) {
            if (!tableExists(connection, entry.getKey())) continue;
            for (String column : entry.getValue()) {
                if (columnExists(connection, entry.getKey(), column)) {
                    try (var statement = connection.createStatement()) {
                        statement.execute("ALTER TABLE \"" + entry.getKey() + "\" DROP COLUMN \"" + column + "\"");
                    }
                }
            }
        }
    }

    private static boolean tableExists(Connection connection, String table) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?")) {
            statement.setString(1, table);
            try (var rows = statement.executeQuery()) {
                return rows.next();
            }
        }
    }

    private static boolean columnExists(Connection connection, String table, String column) throws SQLException {
        try (var statement = connection.createStatement();
             var rows = statement.executeQuery("PRAGMA table_info(\"" + table + "\")")) {
            while (rows.next()) {
                if (column.equalsIgnoreCase(rows.getString("name"))) return true;
            }
            return false;
        }
    }
}
