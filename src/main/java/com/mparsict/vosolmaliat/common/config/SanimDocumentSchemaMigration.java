package com.mparsict.vosolmaliat.common.config;

import java.sql.*;

/** Additive, idempotent migration. Never derives historical source facts from legacy totals. */
final class SanimDocumentSchemaMigration {
    private SanimDocumentSchemaMigration() {}

    static void migrate(Connection connection) throws SQLException {
        if (tableExists(connection, "finalized_debt_paper")) {
            add(connection, "finalized_debt_paper", "paper_type", "TEXT");
            add(connection, "finalized_debt_paper", "paper_name", "TEXT");
            add(connection, "finalized_debt_paper", "source_mapping_version", "TEXT");
            add(connection, "finalized_debt_paper", "source_document_number", "TEXT");
            add(connection, "finalized_debt_paper", "source_issue_date_raw", "TEXT");
            add(connection, "finalized_debt_paper", "source_basis_document_number", "TEXT");
            add(connection, "finalized_debt_paper", "source_basis_document_date_raw", "TEXT");
            add(connection, "finalized_debt_paper", "source_issuance_reason", "TEXT");
            add(connection, "finalized_debt_paper", "source_tax_year", "TEXT");
            add(connection, "finalized_debt_paper", "source_tax_period", "TEXT");
            add(connection, "finalized_debt_paper", "source_tax_period_start_raw", "TEXT");
            add(connection, "finalized_debt_paper", "source_tax_period_end_raw", "TEXT");
            add(connection, "finalized_debt_paper", "source_taxpayer_identifier", "TEXT");
            add(connection, "finalized_debt_paper", "source_taxpayer_identifier_type", "TEXT");
            add(connection, "finalized_debt_paper", "source_identity_number", "TEXT");
            add(connection, "finalized_debt_paper", "source_identity_type", "TEXT");
            add(connection, "finalized_debt_paper", "source_person_type", "TEXT");
            add(connection, "finalized_debt_paper", "source_taxpayer_name", "TEXT");
            add(connection, "finalized_debt_paper", "source_trade_name", "TEXT");
            add(connection, "finalized_debt_paper", "source_activity_title", "TEXT");
            add(connection, "finalized_debt_paper", "source_phone", "TEXT");
            add(connection, "finalized_debt_paper", "source_postal_code", "TEXT");
            add(connection, "finalized_debt_paper", "source_address", "TEXT");
            add(connection, "finalized_debt_paper", "source_general_directorate_name", "TEXT");
            add(connection, "finalized_debt_paper", "source_tax_office_name", "TEXT");
            add(connection, "finalized_debt_paper", "source_tax_office_code", "TEXT");
            add(connection, "finalized_debt_paper", "source_currency_code", "TEXT");
            add(connection, "finalized_debt_paper", "source_tax_amount_basis", "TEXT");
            add(connection, "finalized_debt_paper", "source_remission_basis", "TEXT");
            add(connection, "finalized_debt_paper", "source_validation_status", "TEXT");
            add(connection, "finalized_debt_paper", "aggregate_outstanding_debt_amount", "TEXT");
            add(connection, "finalized_debt_paper", "aggregate_total_tax_amount", "TEXT");
            add(connection, "finalized_debt_paper", "aggregate_total_duty_amount", "TEXT");
            add(connection, "finalized_debt_paper", "aggregate_total_penalty_amount", "TEXT");
            add(connection, "finalized_debt_paper", "aggregate_total_paid_amount", "TEXT");
            add(connection, "finalized_debt_paper", "aggregate_total_credit_amount", "TEXT");
            add(connection, "finalized_debt_paper", "aggregate_total_remission_amount", "TEXT");
            add(connection, "finalized_debt_paper", "taxpayer_id", "INTEGER");
            add(connection, "finalized_debt_paper", "tax_return_number", "TEXT");
            add(connection, "finalized_debt_paper", "tax_return_date", "TEXT");
            add(connection, "finalized_debt_paper", "final_tax_debt_notification_number", "TEXT");
            add(connection, "finalized_debt_paper", "final_tax_debt_notification_view_date", "TEXT");
            add(connection, "finalized_debt_paper", "tax_year", "TEXT");
            add(connection, "finalized_debt_paper", "tax_period", "TEXT");
            add(connection, "finalized_debt_paper", "period_start_date", "TEXT");
            add(connection, "finalized_debt_paper", "period_end_date", "TEXT");
            add(connection, "finalized_debt_paper", "tax_affairs_office_id", "INTEGER");
            migratePaperType(connection);
            backfillFinalizedDebtPaper(connection);
        }
        if (tableExists(connection, "debt_information")) {
            add(connection, "debt_information", "version", "INTEGER DEFAULT 0");
            add(connection, "debt_information", "aggregate_outstanding_debt_amount", "TEXT");
            add(connection, "debt_information", "aggregate_total_tax_amount", "TEXT");
            add(connection, "debt_information", "aggregate_total_duty_amount", "TEXT");
            add(connection, "debt_information", "aggregate_total_penalty_amount", "TEXT");
            add(connection, "debt_information", "aggregate_total_paid_amount", "TEXT");
            add(connection, "debt_information", "aggregate_total_credit_amount", "TEXT");
            add(connection, "debt_information", "aggregate_total_remission_amount", "TEXT");
            add(connection, "debt_information", "current_amounts_as_of", "TIMESTAMP");
        }
        if (tableExists(connection, "debt_inquiry_result")) {
            add(connection, "debt_inquiry_result", "aggregate_outstanding_debt_amount", "TEXT");
            add(connection, "debt_inquiry_result", "aggregate_total_tax_amount", "TEXT");
            add(connection, "debt_inquiry_result", "aggregate_total_duty_amount", "TEXT");
            add(connection, "debt_inquiry_result", "aggregate_total_penalty_amount", "TEXT");
            add(connection, "debt_inquiry_result", "aggregate_total_paid_amount", "TEXT");
            add(connection, "debt_inquiry_result", "aggregate_total_credit_amount", "TEXT");
            add(connection, "debt_inquiry_result", "aggregate_total_remission_amount", "TEXT");
            add(connection, "debt_inquiry_result", "effective_at", "TIMESTAMP");
            add(connection, "debt_inquiry_result", "mapping_version", "TEXT");
        }
        if (tableExists(connection, "debt_paper_push_record")) {
            add(connection, "debt_paper_push_record", "announcement_type", "TEXT");
            add(connection, "debt_paper_push_record", "source_tax_office_code", "TEXT");
            add(connection, "debt_paper_push_record", "mapping_version", "TEXT");
            add(connection, "debt_paper_push_record", "source_document_id", "INTEGER");
        }
        if (tableExists(connection, "execution_paper")) {
            add(connection, "execution_paper", "old_execution_letter_number", "TEXT");
            add(connection, "execution_paper", "old_execution_letter_date", "TEXT");
        }
        if (tableExists(connection, "execution_paper_notification")) {
            add(connection, "execution_paper_notification", "notification_date", "TEXT");
        }
        if (tableExists(connection, "taxpayer")) {
            add(connection, "taxpayer", "taxpayer_type", "TEXT");
            if (columnExists(connection, "taxpayer", "natural_person_id")
                    && columnExists(connection, "taxpayer", "legal_person_id")) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("UPDATE taxpayer SET taxpayer_type = CASE "
                            + "WHEN natural_person_id IS NOT NULL THEN 'NATURAL' "
                            + "WHEN legal_person_id IS NOT NULL THEN 'LEGAL' ELSE taxpayer_type END "
                            + "WHERE taxpayer_type IS NULL");
                }
            }
        }
        addRuleColumns(connection, "execution_paper_rejection_rule", "ISSUANCE");
        addRuleColumns(connection, "execution_paper_inform_rejection_rule", "INFORM");
        migrateRelatedPersons(connection);
    }

    private static void backfillFinalizedDebtPaper(Connection connection) throws SQLException {
        if (!tableExists(connection, "execution_paper")) return;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE finalized_debt_paper SET taxpayer_id = ("
                    + "SELECT ep.taxpayer_id FROM execution_paper ep "
                    + "WHERE ep.finalized_debt_paper_id = finalized_debt_paper.id LIMIT 1) "
                    + "WHERE taxpayer_id IS NULL");
            statement.executeUpdate("UPDATE finalized_debt_paper SET "
                    + "tax_return_number=COALESCE(tax_return_number,source_basis_document_number),"
                    + "tax_return_date=COALESCE(tax_return_date,source_basis_document_date_raw),"
                    + "final_tax_debt_notification_number=COALESCE(final_tax_debt_notification_number,source_document_number),"
                    + "tax_year=COALESCE(tax_year,source_tax_year),tax_period=COALESCE(tax_period,source_tax_period),"
                    + "period_start_date=COALESCE(period_start_date,source_tax_period_start_raw),"
                    + "period_end_date=COALESCE(period_end_date,source_tax_period_end_raw)");
        }
    }

    private static void addRuleColumns(Connection connection, String table, String scope) throws SQLException {
        if (!tableExists(connection, table)) return;
        add(connection, table, "version", "INTEGER DEFAULT 0");
        add(connection, table, "rule_scope", "TEXT");
        add(connection, table, "parent_rule_id", "INTEGER");
        add(connection, table, "error_code", "TEXT");
        add(connection, table, "error_message", "TEXT");
        add(connection, table, "is_active", "BOOLEAN DEFAULT 1");
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE " + table + " SET rule_scope='" + scope
                    + "' WHERE rule_scope IS NULL");
            statement.executeUpdate("UPDATE " + table + " SET is_active=1 WHERE is_active IS NULL");
        }
    }

    private static void migrateRelatedPersons(Connection connection) throws SQLException {
        if (!tableExists(connection, "related_persons")) return;
        add(connection, "related_persons", "taxpayer_id", "INTEGER");
        add(connection, "related_persons", "is_board_member", "BOOLEAN");
        add(connection, "related_persons", "is_partner", "BOOLEAN");
        try (Statement statement = connection.createStatement()) {
            if (columnExists(connection, "related_persons", "owner_legal_person_id")) {
                statement.executeUpdate("UPDATE related_persons SET taxpayer_id=(SELECT taxpayer.id FROM taxpayer "
                        + "WHERE taxpayer.legal_person_id=related_persons.owner_legal_person_id LIMIT 1) "
                        + "WHERE taxpayer_id IS NULL");
            }
            if (columnExists(connection, "related_persons", "is_non_board_member")) {
                statement.executeUpdate("UPDATE related_persons SET is_board_member="
                        + "CASE WHEN is_non_board_member IS NULL THEN NULL WHEN is_non_board_member=0 THEN 1 ELSE 0 END "
                        + "WHERE is_board_member IS NULL");
            }
            if (columnExists(connection, "related_persons", "is_non_partner")) {
                statement.executeUpdate("UPDATE related_persons SET is_partner="
                        + "CASE WHEN is_non_partner IS NULL THEN NULL WHEN is_non_partner=0 THEN 1 ELSE 0 END "
                        + "WHERE is_partner IS NULL");
            }
        }
    }

    private static void migratePaperType(Connection connection) throws SQLException {
        if (columnExists(connection, "finalized_debt_paper", "source_template_code")) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("UPDATE finalized_debt_paper SET paper_type = "
                        + "CASE UPPER(source_template_code) "
                        + "WHEN 'LTR_EF03' THEN 'EF03' WHEN 'EF03' THEN 'EF03' "
                        + "WHEN 'LTR_EF23' THEN 'EF23' WHEN 'EF23' THEN 'EF23' "
                        + "WHEN 'LTR_EF23_1' THEN 'EF23_1' WHEN 'EF23_1' THEN 'EF23_1' "
                        + "WHEN 'LTR_EF23_2' THEN 'EF23_2' WHEN 'EF23_2' THEN 'EF23_2' "
                        + "WHEN 'LTR_EF104' THEN 'EF104' WHEN 'EF104' THEN 'EF104' ELSE paper_type END "
                        + "WHERE source_template_code IS NOT NULL");
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE finalized_debt_paper SET paper_name = CASE paper_type "
                    + "WHEN 'EF03' THEN 'برگ اجرایی' "
                    + "WHEN 'EF23' THEN 'برگ قطعی مالیات و عوارض ارزش افزوده' "
                    + "WHEN 'EF23_1' THEN 'برگ قطعی مالیات و عوارض ارزش افزوده ـ نوع ۱' "
                    + "WHEN 'EF23_2' THEN 'برگ قطعی مالیات و عوارض ارزش افزوده ـ نوع ۲' "
                    + "WHEN 'EF104' THEN 'برگ قطعی جرائم مالیاتی' ELSE paper_name END "
                    + "WHERE paper_type IS NOT NULL");
        }
    }

    private static boolean tableExists(Connection connection, String table) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?")) {
            statement.setString(1, table);
            try (ResultSet rows = statement.executeQuery()) { return rows.next(); }
        }
    }

    private static void add(Connection connection, String table, String column, String type) throws SQLException {
        // Identifiers are exclusively compile-time constants above.
        try (Statement statement = connection.createStatement()) {
            try (ResultSet rows = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
                while (rows.next()) if (column.equals(rows.getString("name"))) return;
            }
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
        }
    }

    private static boolean columnExists(Connection connection, String table, String column) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rows.next()) if (column.equals(rows.getString("name"))) return true;
            return false;
        }
    }
}
