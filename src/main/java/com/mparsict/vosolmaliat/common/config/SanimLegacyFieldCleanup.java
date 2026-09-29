package com.mparsict.vosolmaliat.common.config;

import java.sql.*;
import java.nio.file.Path;
import java.util.*;

/** Removes superseded columns and the obsolete tax-unit organization level. */
final class SanimLegacyFieldCleanup {
    private SanimLegacyFieldCleanup() {}
    static final Map<String, List<String>> COLUMNS = Map.of(
            "finalized_debt_paper", List.of("debt_paper_issue_date", "notification_id", "finalization_reason",
                    "performance_year", "performance_period", "activity_type_code", "activity_type_title",
                    "total_finalized_amount", "finalized_tax_and_non_deductible_amount", "source_template_code",
                    "tax_unit_id", "tax_bureau_id"),
            "debt_information", List.of("tax_amount", "duty_amount", "penalty_article_22", "penalty_article_23",
                    "deductible_penalty", "non_deductible_penalty", "tax_paid_amount", "penalty_paid_amount",
                    "tax_credit", "remaining_tax", "municipality_share", "guild_share", "compliance_bonus",
                    "sanim_total_debt_rials", "sanim_total_penalty_rials", "sanim_total_payment_rials",
                    "sanim_total_credit_rials", "sanim_outstanding_debt_rials", "sanim_last_activity_at"),
            "execution_paper", List.of("finalization_reason", "tax_unit_id", "sending_unit_id"),
            "app_user_profile", List.of("tax_unit_id"),
            "debt_paper_push_record", List.of("tax_office_id"),
            "related_persons", List.of("is_non_board_member", "is_non_partner"));

    static void migrate(Connection connection) throws SQLException {
        boolean needed = false;
        for (var entry : COLUMNS.entrySet()) {
            var present = columns(connection, entry.getKey());
            if (entry.getValue().stream().anyMatch(present::contains)) needed = true;
        }
        if (!needed) return;
        backup(connection); // Failure to create a recoverable copy prevents every DROP.
        connection.setAutoCommit(false);
        try {
            var documentColumns = columns(connection, "finalized_debt_paper");
            if (!documentColumns.isEmpty()) {
                copyText(connection, "finalized_debt_paper", "performance_year", "source_tax_year");
                copyText(connection, "finalized_debt_paper", "performance_period", "source_tax_period");
                copyText(connection, "finalized_debt_paper", "activity_type_title", "source_activity_title");
                try (var statement = connection.createStatement()) {
                    statement.executeUpdate("UPDATE finalized_debt_paper SET source_validation_status='LEGACY_REVIEW_REQUIRED' "
                            + "WHERE source_document_number IS NULL OR source_issue_date_raw IS NULL OR aggregate_outstanding_debt_amount IS NULL");
                }
            }
            copyText(connection, "debt_paper_push_record", "tax_office_id", "source_tax_office_code");
            copyText(connection, "finalized_debt_paper", "tax_bureau_id", "tax_affairs_office_id");
            for (var entry : COLUMNS.entrySet()) {
                var present = columns(connection, entry.getKey());
                for (String column : entry.getValue()) {
                    if (!present.contains(column)) continue;
                    try (var statement = connection.createStatement()) {
                        statement.execute("ALTER TABLE " + entry.getKey() + " DROP COLUMN " + column);
                    }
                }
            }
            try (var statement = connection.createStatement()) {
                statement.execute("DROP TABLE IF EXISTS tax_unit");
            }
            connection.commit();
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        } finally { connection.setAutoCommit(true); }
    }

    private static void backup(Connection connection) throws SQLException {
        String file = null;
        try (var statement = connection.createStatement(); var rows = statement.executeQuery("PRAGMA database_list")) {
            while (rows.next()) if ("main".equals(rows.getString("name"))) file = rows.getString("file");
        }
        if (file == null || file.isBlank()) throw new SQLException("Legacy field cleanup requires a file database and a durable backup.");
        Path original = Path.of(file);
        String target = original.resolveSibling(original.getFileName() + ".pre-sanim-cleanup-" + UUID.randomUUID() + ".sqlite").toString();
        try (var statement = connection.prepareStatement("VACUUM INTO ?")) {
            statement.setString(1, target);
            statement.execute();
        }
    }

    private static void copyText(Connection connection, String table, String from, String to) throws SQLException {
        var present = columns(connection, table);
        if (!present.contains(from) || !present.contains(to)) return;
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE " + table + " SET " + to + "=CAST(" + from + " AS TEXT) WHERE " + to + " IS NULL");
        }
    }

    private static Set<String> columns(Connection connection, String table) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (var statement = connection.createStatement(); var rows = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rows.next()) columns.add(rows.getString("name"));
        }
        return columns;
    }
}
