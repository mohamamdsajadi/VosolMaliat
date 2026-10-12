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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class SqliteLegacySchemaMigratorUnitTest {

    @TempDir
    Path tempDirectory;

    @Test
    void migratesDocumentEnumsAndCorrectedColumnNamesWithoutLosingValues() throws Exception {
        String jdbcUrl = "jdbc:sqlite:" + tempDirectory.resolve("document-model.sqlite");
        try (Connection connection=DriverManager.getConnection(jdbcUrl);Statement statement=connection.createStatement()) {
            statement.execute("CREATE TABLE paper_type (id INTEGER PRIMARY KEY, paper_type TEXT UNIQUE, paper_title TEXT)");
            statement.execute("INSERT INTO paper_type VALUES (1,'EF03','برگ اجرایی')");
            statement.execute("""
                    CREATE TABLE final_tax_debt_paper (id INTEGER PRIMARY KEY,created_at TIMESTAMP,created_by TEXT,
                    creator_user_id INTEGER,updated_at TIMESTAMP,updated_by TEXT,updater_user_id INTEGER,
                    paper_type TEXT,finalization_reason TEXT,tax_source TEXT,tax_period TEXT,national_code TEXT,national_id TEXT,
                    unique_final_tax_debt_notice_id TEXT,tax_return_number TEXT,final_tax_debt_notice_issue_date TIMESTAMP,
                    tax_return_date TIMESTAMP,final_tax_debt_paper_notification_number TEXT,
                    final_tax_debt_paper_notification_view_date TIMESTAMP,taxpayer_id INTEGER,tax_year INTEGER,
                    period_start_date TIMESTAMP,period_end_date TIMESTAMP,tax_affairs_office_id INTEGER)
                    """);
            statement.execute("""
                    INSERT INTO final_tax_debt_paper VALUES
                    (1,CURRENT_TIMESTAMP,'SYSTEM',0,CURRENT_TIMESTAMP,'SYSTEM',0,'EF03','FINAL_NOTICE','INCOME_TAX','SPRING',
                    '001','002','debt-1',NULL,CURRENT_TIMESTAMP,NULL,'notice-1',NULL,1,1404,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1)
                    """);
            statement.execute("""
                    CREATE TABLE taxpayer (id INTEGER PRIMARY KEY,creation_date TIMESTAMP,created_by TEXT,creator_user_id INTEGER,
                    updated_at TIMESTAMP,updated_by TEXT,updater_user_id INTEGER,registeration_trace_code TEXT,taxpayer_type TEXT,
                    taxpayer_id TEXT,natural_person_id INTEGER,legal_person_id INTEGER)
                    """);
            statement.execute("INSERT INTO taxpayer VALUES (1,CURRENT_TIMESTAMP,'SYSTEM',0,CURRENT_TIMESTAMP,'SYSTEM',0,'follow-1','NATURAL','tin-1',10,NULL)");
            statement.execute("""
                    CREATE TABLE restrictions_applied_to_companies (id INTEGER PRIMARY KEY,creation_date TIMESTAMP,created_by TEXT,
                    creator_user_id INTEGER,updated_at TIMESTAMP,updated_by TEXT,updater_user_id INTEGER,
                    finalized_debt_paper_notification_id TEXT,natural_person_id INTEGER,legal_person_id INTEGER,result BOOLEAN)
                    """);
            statement.execute("INSERT INTO restrictions_applied_to_companies VALUES (1,CURRENT_TIMESTAMP,'SYSTEM',0,CURRENT_TIMESTAMP,'SYSTEM',0,'17',10,NULL,1)");
            statement.execute("""
                    CREATE TABLE related_person_post (id INTEGER PRIMARY KEY,created_at TIMESTAMP,created_by TEXT,
                    creator_user_id INTEGER,updated_at TIMESTAMP,updated_by TEXT,updater_user_id INTEGER,code TEXT)
                    """);
            statement.execute("INSERT INTO related_person_post VALUES (1,CURRENT_TIMESTAMP,'SYSTEM',0,CURRENT_TIMESTAMP,'SYSTEM',0,'CEO')");
            statement.execute("""
                    CREATE TABLE related_persons (id INTEGER PRIMARY KEY,creation_date TIMESTAMP,created_by TEXT,
                    creator_user_id INTEGER,updated_at TIMESTAMP,updated_by TEXT,updater_user_id INTEGER,
                    owner_legal_person_id INTEGER,related_natural_person_id INTEGER,related_legal_person_id INTEGER,
                    natural_person_related_to_legal_person_id INTEGER,post_id INTEGER,post_name TEXT,taxpayer_id INTEGER,
                    start_date TEXT,end_date TEXT,position_expiry_date TEXT,is_board_member BOOLEAN,is_partner BOOLEAN,
                    has_signatory_authority BOOLEAN,management_declaration INTEGER,source_system TEXT,source_record_id TEXT,last_synced_at TEXT)
                    """);
            statement.execute("""
                    INSERT INTO related_persons VALUES
                    (1,CURRENT_TIMESTAMP,'SYSTEM',0,CURRENT_TIMESTAMP,'SYSTEM',0,20,10,NULL,NULL,1,'CEO',1,
                    '1404-01-01','1404-12-29','1404-12-29',1,0,1,NULL,'TEST','row-1',CURRENT_TIMESTAMP)
                    """);
        }

        SqliteLegacySchemaMigrator.migrate(jdbcUrl);
        SqliteLegacySchemaMigrator.migrate(jdbcUrl);

        try (Connection connection=DriverManager.getConnection(jdbcUrl);Statement statement=connection.createStatement()) {
            assertThat(columns(statement,"final_tax_debt_paper")).contains("paper_type_id","finalization_reason_id","tax_period_id")
                    .doesNotContain("tax_source_id")
                    .doesNotContain("paper_type","finalization_reason","tax_source","tax_period","national_code","national_id");
            assertThat(columns(statement,"taxpayer")).contains("follow_code","taxpayer_type_id")
                    .doesNotContain("registeration_trace_code","taxpayer_type");
            assertThat(columns(statement,"restrictions_applied_to_companies"))
                    .contains("execution_paper_id","restriction_state_id","restriction_tracking_code","release_tracking_code")
                    .doesNotContain("finalized_debt_paper_notification_id","legal_person_id","result");
            assertThat(columns(statement,"related_persons")).contains("owner_legal_person_id","post_id","start_date")
                    .doesNotContain("taxpayer_id","post_name");
            assertThat(columns(statement,"paper_type")).contains("tax_source_id");
            assertThat(columns(statement,"post_in_company"))
                    .contains("code_in_company_registration_system","title")
                    .doesNotContain("code");
            assertThat(tableExists(statement,"related_person_post")).isFalse();
            try (ResultSet row=statement.executeQuery("SELECT paper_type_id,finalization_reason_id,tax_period_id FROM final_tax_debt_paper")) {
                assertThat(row.next()).isTrue(); assertThat(row.getInt(1)).isEqualTo(1); assertThat(row.getInt(2)).isEqualTo(1);
                assertThat(row.getInt(3)).isEqualTo(1);
            }
            try (ResultSet row=statement.executeQuery("SELECT follow_code,taxpayer_type_id FROM taxpayer")) {
                assertThat(row.next()).isTrue(); assertThat(row.getString(1)).isEqualTo("follow-1"); assertThat(row.getInt(2)).isEqualTo(2);
            }
            try (ResultSet row=statement.executeQuery("SELECT start_date FROM related_persons")) {
                assertThat(row.next()).isTrue();
                assertThat(row.getString(1)).startsWith("2025-03-21");
            }
            try (ResultSet row=statement.executeQuery("SELECT tax_source_id FROM paper_type WHERE paper_type='EF03'")) {
                assertThat(row.next()).isTrue(); assertThat(row.getInt(1)).isEqualTo(2);
            }
            try (ResultSet row=statement.executeQuery("SELECT code_in_company_registration_system,title FROM post_in_company")) {
                assertThat(row.next()).isTrue(); assertThat(row.getString(1)).isEqualTo("001");
                assertThat(row.getString(2)).isEqualTo("مدیرعامل");
            }
            try (ResultSet row=statement.executeQuery("SELECT unique_final_tax_debt_notice_id,tax_return_number,final_tax_debt_notice_issue_date,tax_return_date FROM final_tax_debt_paper")) {
                assertThat(row.next()).isTrue(); assertThat(row.getString(1)).isEqualTo("debt-1");
                assertThat(row.getString(2)).isNull(); assertThat(row.getString(3)).isNotNull();
                assertThat(row.getString(4)).isNull();
            }
        }
    }

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
                    .contains("finalized_debt_paper_notification_id", "xml", "hash")
                    .doesNotContain("request_id", "execution_paper_id", "received_at", "response_json");
            try (ResultSet resultSet = statement.executeQuery(
                    "SELECT finalized_debt_paper_notification_id FROM debt_inquiry_result")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString(1)).isEqualTo("legacy-1");
            }
        }
    }

    @Test
    void relaxesOnlyEf03OptionalFieldsAndIsIdempotent() throws Exception {
        String jdbcUrl = "jdbc:sqlite:" + tempDirectory.resolve("ef03-legacy.sqlite");
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE natural_person_extra_info (id INTEGER PRIMARY KEY, birth_date TIMESTAMP NOT NULL, father_name VARCHAR(255) NOT NULL, full_name VARCHAR(255) NOT NULL)");
            statement.execute("CREATE TABLE legal_person_extra_info (id INTEGER PRIMARY KEY, registration_date TIMESTAMP NOT NULL, registration_number VARCHAR(255) NOT NULL UNIQUE, trade_name VARCHAR(255) NOT NULL, name VARCHAR(255) NOT NULL)");
            statement.execute("CREATE TABLE contact_info (id INTEGER PRIMARY KEY, phone_no VARCHAR(255) NOT NULL, mobile_no VARCHAR(255) NOT NULL, address VARCHAR(255) NOT NULL)");
        }

        SqliteLegacySchemaMigrator.migrate(jdbcUrl);
        SqliteLegacySchemaMigrator.migrate(jdbcUrl);

        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement statement = connection.createStatement()) {
            assertThat(nullableState(statement, "natural_person_extra_info"))
                    .containsEntry("birth_date", 0).containsEntry("father_name", 0)
                    .containsEntry("full_name", 1);
            assertThat(nullableState(statement, "legal_person_extra_info"))
                    .containsEntry("registration_date", 0).containsEntry("registration_number", 0)
                    .containsEntry("trade_name", 0).containsEntry("name", 1);
            assertThat(nullableState(statement, "contact_info"))
                    .containsEntry("phone_no", 0).containsEntry("mobile_no", 0)
                    .containsEntry("address", 0);
        }
    }

    private Set<String> columns(Statement statement) throws Exception {
        return columns(statement, "debt_inquiry_result");
    }

    private Set<String> columns(Statement statement, String table) throws Exception {
        Set<String> columns = new HashSet<>();
        try (ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (resultSet.next()) {
                columns.add(resultSet.getString("name"));
            }
        }
        return columns;
    }

    private Map<String, Integer> nullableState(Statement statement, String table) throws Exception {
        try (ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            var entries = new java.util.ArrayList<Map.Entry<String, Integer>>();
            while (resultSet.next()) {
                entries.add(Map.entry(resultSet.getString("name"), resultSet.getInt("notnull")));
            }
            return entries.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }

    private boolean tableExists(Statement statement, String table) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name='" + table.replace("'", "''") + "'")) {
            return resultSet.next();
        }
    }
}
