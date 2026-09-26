package com.mparsict.vosolmaliat.common.config;

import java.sql.*;

/** Additive, idempotent migration. Never derives historical source facts from legacy totals. */
final class SanimDocumentSchemaMigration {
    private SanimDocumentSchemaMigration() {}

    static void migrate(Connection connection) throws SQLException {
        if (tableExists(connection, "finalized_debt_paper")) {
            add(connection, "finalized_debt_paper", "source_template_code", "TEXT");
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
}
