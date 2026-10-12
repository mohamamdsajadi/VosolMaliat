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
        REMOVED_COLUMNS.put("final_tax_debt_paper", List.of("paper_type", "finalization_reason",
                "tax_source", "tax_source_id", "tax_period", "national_code", "national_id"));
        REMOVED_COLUMNS.put("taxpayer", List.of("registeration_trace_code", "taxpayer_type"));
        REMOVED_COLUMNS.put("restrictions_applied_to_companies", List.of(
                "finalized_debt_paper_notification_id", "legal_person_id", "result"));
        REMOVED_COLUMNS.put("related_persons", List.of("taxpayer_id", "post_name", "is_board_member",
                "is_partner", "has_signatory_authority", "management_declaration", "source_system",
                "source_record_id", "last_synced_at"));
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
        REMOVED_COLUMNS.put("execution_paper_state_history", List.of("state_date"));
        REMOVED_COLUMNS.put("tax_affairs_office_history", List.of("office_type", "office_code"));
    }

    static void migrate(Connection connection) throws SQLException {
        rebuildDebtInformation(connection);
        rebuildDebtInformationHistory(connection);
        rebuildDebtInquiryResult(connection);
        rebuildIssuanceRequest(connection);
        rebuildTaxAffairsOffice(connection);
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

    private static void rebuildTaxAffairsOffice(Connection connection) throws SQLException {
        String table="tax_affairs_office";
        if (!tableExists(connection,table) || (!columnExists(connection,table,"office_type")
                && !columnExists(connection,table,"office_code"))) return;
        rebuild(connection,table,"""
                CREATE TABLE tax_affairs_office__document_model (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  created_at TIMESTAMP NOT NULL, created_by VARCHAR(255) NOT NULL,
                  creator_user_id BIGINT NOT NULL, updated_at TIMESTAMP NOT NULL,
                  updated_by VARCHAR(255) NOT NULL, updater_user_id BIGINT NOT NULL,
                  general_directorate_of_tax_affairs_id BIGINT NOT NULL,
                  office_name VARCHAR(255) NOT NULL, zipcode VARCHAR(255) NOT NULL,
                  address VARCHAR(255) NOT NULL, phone VARCHAR(255) NOT NULL,
                  FOREIGN KEY(general_directorate_of_tax_affairs_id) REFERENCES general_directorate_of_tax_affairs(id),
                  FOREIGN KEY(creator_user_id) REFERENCES app_user_profile(id),
                  FOREIGN KEY(updater_user_id) REFERENCES app_user_profile(id))
                ""","""
                INSERT INTO tax_affairs_office__document_model
                (id,created_at,created_by,creator_user_id,updated_at,updated_by,updater_user_id,
                 general_directorate_of_tax_affairs_id,office_name,zipcode,address,phone)
                SELECT id,created_at,created_by,creator_user_id,updated_at,updated_by,updater_user_id,
                 general_directorate_of_tax_affairs_id,office_name,zipcode,address,phone
                 FROM tax_affairs_office
                """);
    }

    private static void rebuildDebtInformation(Connection connection) throws SQLException {
        String table="debt_information";
        if (!tableExists(connection,table) || !columnExists(connection,table,"debt_paper_unique_id")
                || (!columnExists(connection,table,"execution_paper_id")
                && !columnExists(connection,table,"current_amounts_as_of")
                && !columnExists(connection,table,"tax_component"))) return;
        String liabilityDetails = columnExists(connection, table, "tax_liability_details")
                ? "tax_liability_details" : "NULL";
        String outstandingDetails = columnExists(connection, table, "outstanding_debt_details")
                ? "outstanding_debt_details" : "NULL";
        rebuild(connection,table,"""
                CREATE TABLE debt_information__document_model (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  created_at TIMESTAMP NOT NULL, created_by VARCHAR(255) NOT NULL,
                  creator_user_id BIGINT NOT NULL, updated_at TIMESTAMP NOT NULL,
                  updated_by VARCHAR(255) NOT NULL, updater_user_id BIGINT NOT NULL,
                  debt_paper_unique_id BIGINT NOT NULL, total_liability TEXT NOT NULL,
                  total_penalties TEXT NOT NULL, total_credits TEXT NOT NULL,
                  total_payments TEXT NOT NULL, total_remissions TEXT NOT NULL,
                  outstanding_tax_debt TEXT NOT NULL, penalties_detail TEXT, credits_detail TEXT,
                  payments_detail TEXT, remissions_detail TEXT, tax_liability_details TEXT,
                  outstanding_debt_details TEXT, version BIGINT,
                  FOREIGN KEY(debt_paper_unique_id) REFERENCES final_tax_debt_paper(id))
                ""","""
                INSERT INTO debt_information__document_model
                (id,created_at,created_by,creator_user_id,updated_at,updated_by,updater_user_id,
                 debt_paper_unique_id,total_liability,total_penalties,total_credits,total_payments,
                 total_remissions,outstanding_tax_debt,penalties_detail,credits_detail,payments_detail,
                 remissions_detail,tax_liability_details,outstanding_debt_details,version)
                SELECT id,created_at,created_by,creator_user_id,updated_at,updated_by,updater_user_id,
                 debt_paper_unique_id,total_liability,total_penalties,total_credits,total_payments,
                 total_remissions,outstanding_tax_debt,penalties_detail,credits_detail,payments_detail,
                 remissions_detail,""" + liabilityDetails + "," + outstandingDetails
                + ",version FROM debt_information");
    }

    private static void rebuildDebtInformationHistory(Connection connection) throws SQLException {
        String table="debt_information_history";
        if (!tableExists(connection,table) || !columnExists(connection,table,"debt_paper_unique_id")
                || (!columnExists(connection,table,"debt_information_id")
                && !columnExists(connection,table,"execution_paper_id")
                && !columnExists(connection,table,"effective_at"))) return;
        String liabilityDetails = columnExists(connection, table, "tax_liability_details")
                ? "tax_liability_details" : "NULL";
        String outstandingDetails = columnExists(connection, table, "outstanding_debt_details")
                ? "outstanding_debt_details" : "NULL";
        rebuild(connection,table,"""
                CREATE TABLE debt_information_history__document_model (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  creation_date TIMESTAMP NOT NULL, created_by VARCHAR(255) NOT NULL,
                  creator_user_id BIGINT NOT NULL, updated_at TIMESTAMP NOT NULL,
                  updated_by VARCHAR(255) NOT NULL, updater_user_id BIGINT NOT NULL,
                  debt_paper_unique_id BIGINT NOT NULL, total_liability TEXT NOT NULL,
                  total_penalties TEXT NOT NULL, total_credits TEXT NOT NULL,
                  total_payments TEXT NOT NULL, total_remissions TEXT NOT NULL,
                  outstanding_tax_debt TEXT NOT NULL, penalties_detail TEXT, credits_detail TEXT,
                  payments_detail TEXT, remissions_detail TEXT, tax_liability_details TEXT,
                  outstanding_debt_details TEXT, next_inquiry_date TIMESTAMP,
                  FOREIGN KEY(debt_paper_unique_id) REFERENCES final_tax_debt_paper(id))
                ""","""
                INSERT INTO debt_information_history__document_model
                (id,creation_date,created_by,creator_user_id,updated_at,updated_by,updater_user_id,
                 debt_paper_unique_id,total_liability,total_penalties,total_credits,total_payments,
                 total_remissions,outstanding_tax_debt,penalties_detail,credits_detail,payments_detail,
                 remissions_detail,tax_liability_details,outstanding_debt_details,next_inquiry_date)
                SELECT id,creation_date,created_by,creator_user_id,updated_at,updated_by,updater_user_id,
                 debt_paper_unique_id,total_liability,total_penalties,total_credits,total_payments,
                 total_remissions,outstanding_tax_debt,penalties_detail,credits_detail,payments_detail,
                 remissions_detail,""" + liabilityDetails + "," + outstandingDetails
                + ",next_inquiry_date FROM debt_information_history");
    }

    private static void rebuildDebtInquiryResult(Connection connection) throws SQLException {
        String table="debt_inquiry_result";
        if (!tableExists(connection,table) || !columnExists(connection,table,"hash")
                || !columnExists(connection,table,"finalized_debt_paper_notification_id")
                || (!columnExists(connection,table,"request_id")
                && !columnExists(connection,table,"execution_paper_id")
                && !columnExists(connection,table,"received_at")
                && !columnExists(connection,table,"response_json")
                && !columnExists(connection,table,"mapping_version")
                && !columnExists(connection,table,"effective_at"))) return;
        String auditColumns = columnExists(connection,table,"created_at")
                ? "created_at,created_by,creator_user_id,updated_at,updated_by,updater_user_id"
                : "CURRENT_TIMESTAMP,'SYSTEM',0,CURRENT_TIMESTAMP,'SYSTEM',0";
        String payloadColumn = columnExists(connection,table,"xml") ? "xml" : "response_json";
        rebuild(connection,table,"""
                CREATE TABLE debt_inquiry_result__document_model (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  created_at TIMESTAMP NOT NULL, created_by VARCHAR(255) NOT NULL,
                  creator_user_id BIGINT NOT NULL, updated_at TIMESTAMP NOT NULL,
                  updated_by VARCHAR(255) NOT NULL, updater_user_id BIGINT NOT NULL,
                  finalized_debt_paper_notification_id VARCHAR(255) NOT NULL,
                  xml TEXT NOT NULL, hash VARCHAR(255) NOT NULL,
                  FOREIGN KEY(creator_user_id) REFERENCES app_user_profile(id),
                  FOREIGN KEY(updater_user_id) REFERENCES app_user_profile(id))
                ""","""
                INSERT INTO debt_inquiry_result__document_model
                (id,created_at,created_by,creator_user_id,updated_at,updated_by,updater_user_id,
                 finalized_debt_paper_notification_id,xml,hash)
                SELECT id,""" + auditColumns + ",finalized_debt_paper_notification_id," + payloadColumn
                + ",hash FROM debt_inquiry_result");
    }

    private static void rebuildIssuanceRequest(Connection connection) throws SQLException {
        String table="execution_paper_issuance_request";
        if (!tableExists(connection,table) || (!columnExists(connection,table,"payload")
                && !columnExists(connection,table,"original_request_id"))) return;
        if (columnExists(connection,table,"payload")) {
            try (var statement=connection.createStatement()) {
                statement.executeUpdate("UPDATE execution_paper_issuance_request SET xml=payload WHERE payload IS NOT NULL");
            }
        }
        rebuild(connection,table,"""
                CREATE TABLE execution_paper_issuance_request__document_model (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  created_at TIMESTAMP NOT NULL, created_by VARCHAR(255) NOT NULL,
                  creator_user_id BIGINT NOT NULL, updated_at TIMESTAMP NOT NULL,
                  updated_by VARCHAR(255) NOT NULL, updater_user_id BIGINT NOT NULL,
                  debt_paper_unique_code VARCHAR(255) NOT NULL, xml TEXT NOT NULL,
                  hash VARCHAR(255) NOT NULL, rejection_reason TEXT,
                  execution_paper_issuance_request_rejection_reason_id BIGINT,
                  FOREIGN KEY(creator_user_id) REFERENCES app_user_profile(id),
                  FOREIGN KEY(updater_user_id) REFERENCES app_user_profile(id),
                  FOREIGN KEY(execution_paper_issuance_request_rejection_reason_id) REFERENCES dynamic_control_rule(id))
                ""","""
                INSERT INTO execution_paper_issuance_request__document_model
                (id,created_at,created_by,creator_user_id,updated_at,updated_by,updater_user_id,
                 debt_paper_unique_code,xml,hash,rejection_reason,
                 execution_paper_issuance_request_rejection_reason_id)
                SELECT id,created_at,created_by,creator_user_id,updated_at,updated_by,updater_user_id,
                 debt_paper_unique_code,xml,hash,rejection_reason,
                 execution_paper_issuance_request_rejection_reason_id FROM execution_paper_issuance_request
                """);
    }

    private static void rebuild(Connection connection, String table, String create, String copy) throws SQLException {
        boolean foreignKeys;
        try (var statement=connection.createStatement(); var rows=statement.executeQuery("PRAGMA foreign_keys")) {
            foreignKeys=rows.next() && rows.getInt(1)!=0;
        }
        try (var statement=connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys=OFF");
            statement.execute("DROP TABLE IF EXISTS \""+table+"__document_model\"");
            statement.execute(create);
            statement.execute(copy);
            statement.execute("DROP TABLE \""+table+"\"");
            statement.execute("ALTER TABLE \""+table+"__document_model\" RENAME TO \""+table+"\"");
            statement.execute("PRAGMA foreign_keys="+(foreignKeys ? "ON" : "OFF"));
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
