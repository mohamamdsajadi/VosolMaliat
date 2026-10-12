package com.mparsict.vosolmaliat.common.config;

import com.mparsict.vosolmaliat.domain.enums.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

final class EnumLookupSchemaMigration {
    private EnumLookupSchemaMigration() {}

    static void migrate(Connection connection) throws SQLException {
        boolean foreignKeys=pragma(connection,"foreign_keys") != 0;
        boolean autoCommit=connection.getAutoCommit();
        try(var statement=connection.createStatement()){statement.execute("PRAGMA foreign_keys=OFF");}
        connection.setAutoCommit(false);
        try {
            migrateSchema(connection);
            connection.commit();
        } catch (SQLException | RuntimeException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(autoCommit);
            try(var statement=connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys="+(foreignKeys ? "ON" : "OFF"));
            }
        }
    }

    private static void migrateSchema(Connection connection) throws SQLException {
        createAndSeed(connection, "finalization_reason", FinalizationReason.values());
        createAndSeedTaxSources(connection);
        createAndSeed(connection, "tax_period", TaxPeriod.values());
        createAndSeed(connection, "taxpayer_type", TaxpayerType.values());
        createAndSeed(connection, "restriction_state", RestrictionState.values());
        createAndSeed(connection, "rule_scope", RuleScope.values());
        createAndSeed(connection, "execution_paper_state", ExecutionPaperState.values());

        migratePaperTypes(connection);
        migrateFinalTaxDebtPaper(connection);
        migrateTaxpayer(connection);
        migrateRestrictions(connection);
        migratePostsInCompany(connection);
        migrateRelatedPersons(connection);
        if (tableExists(connection,"post_in_company") && tableExists(connection,"related_person_post")) {
            execute(connection,"DROP TABLE related_person_post");
        }
    }

    private static void createAndSeedTaxSources(Connection connection) throws SQLException {
        if (tableExists(connection,"tax_source") && !columnExists(connection,"tax_source","name")) {
            addColumn(connection,"tax_source","name","VARCHAR(255)");
            if (columnExists(connection,"tax_source","title")) execute(connection,"UPDATE tax_source SET name=title WHERE name IS NULL");
            execute(connection,"DROP TABLE IF EXISTS tax_source__document_model");
            execute(connection,"CREATE TABLE tax_source__document_model (id INTEGER PRIMARY KEY, name VARCHAR(255) NOT NULL)");
            execute(connection,"INSERT INTO tax_source__document_model(id,name) SELECT id,name FROM tax_source");
            replace(connection,"tax_source","tax_source__document_model");
        } else if (!tableExists(connection,"tax_source")) {
            execute(connection,"CREATE TABLE tax_source (id INTEGER PRIMARY KEY, name VARCHAR(255) NOT NULL)");
        }
        try (PreparedStatement statement=connection.prepareStatement(
                "INSERT INTO tax_source(id,name) VALUES (?,?) ON CONFLICT(id) DO UPDATE SET name=excluded.name")) {
            for (TaxSourceType value : TaxSourceType.values()) {
                statement.setInt(1,value.id()); statement.setString(2,value.label()); statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void createAndSeed(Connection connection, String table, LookupValue[] values) throws SQLException {
        try (var statement=connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS " + table
                    + " (id INTEGER PRIMARY KEY, code VARCHAR(64) NOT NULL UNIQUE, title VARCHAR(255) NOT NULL)");
        }
        try (PreparedStatement statement=connection.prepareStatement(
                "INSERT INTO " + table + " (id,code,title) VALUES (?,?,?) ON CONFLICT(id) DO UPDATE SET code=excluded.code,title=excluded.title")) {
            for (LookupValue value : values) {
                statement.setInt(1,value.id());
                statement.setString(2,((Enum<?>)value).name());
                statement.setString(3,value.label());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void migratePaperTypes(Connection connection) throws SQLException {
        String table="paper_type";
        if (!tableExists(connection,table)) return;
        boolean added=!columnExists(connection,table,"tax_source_id");
        addColumn(connection,table,"tax_source_id","INTEGER");
        execute(connection,"""
                UPDATE paper_type SET tax_source_id=CASE paper_type
                  WHEN 'EF03' THEN 2
                  WHEN 'EF23' THEN 1
                  WHEN 'EF23_1' THEN 1
                  WHEN 'EF23_2' THEN 1
                  WHEN 'EF104' THEN 5
                  ELSE tax_source_id END
                WHERE tax_source_id IS NULL
                """);
        requireMapped(connection,table,"paper_type","tax_source_id");
        if (added && columnExists(connection,table,"created_at")) rebuildPaperType(connection);
    }

    private static void rebuildPaperType(Connection connection) throws SQLException {
        execute(connection,"DROP TABLE IF EXISTS paper_type__document_model");
        execute(connection,"""
                CREATE TABLE paper_type__document_model (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  created_at TIMESTAMP NOT NULL, created_by VARCHAR(255) NOT NULL,
                  creator_user_id BIGINT NOT NULL, updated_at TIMESTAMP NOT NULL,
                  updated_by VARCHAR(255) NOT NULL, updater_user_id BIGINT NOT NULL,
                  paper_type VARCHAR(32) NOT NULL UNIQUE, paper_title VARCHAR(255) NOT NULL,
                  tax_source_id INTEGER NOT NULL,
                  FOREIGN KEY(creator_user_id) REFERENCES app_user_profile(id),
                  FOREIGN KEY(updater_user_id) REFERENCES app_user_profile(id),
                  FOREIGN KEY(tax_source_id) REFERENCES tax_source(id))
                """);
        execute(connection,"""
                INSERT INTO paper_type__document_model
                (id,created_at,created_by,creator_user_id,updated_at,updated_by,updater_user_id,
                 paper_type,paper_title,tax_source_id)
                SELECT id,created_at,created_by,creator_user_id,updated_at,updated_by,updater_user_id,
                 paper_type,paper_title,tax_source_id FROM paper_type
                """);
        replace(connection,"paper_type","paper_type__document_model");
    }

    private static void rebuildPostInCompany(Connection connection) throws SQLException {
        execute(connection,"DROP TABLE IF EXISTS post_in_company__document_model");
        execute(connection,"""
                CREATE TABLE post_in_company__document_model (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  created_at TIMESTAMP NOT NULL, created_by VARCHAR(255) NOT NULL,
                  creator_user_id BIGINT NOT NULL, updated_at TIMESTAMP NOT NULL,
                  updated_by VARCHAR(255) NOT NULL, updater_user_id BIGINT NOT NULL,
                  code_in_company_registration_system VARCHAR(64) NOT NULL UNIQUE, title VARCHAR(255) NOT NULL,
                  FOREIGN KEY(creator_user_id) REFERENCES app_user_profile(id),
                  FOREIGN KEY(updater_user_id) REFERENCES app_user_profile(id))
                """);
        execute(connection,"""
                INSERT INTO post_in_company__document_model
                (id,created_at,created_by,creator_user_id,updated_at,updated_by,updater_user_id,
                 code_in_company_registration_system,title)
                SELECT id,created_at,created_by,creator_user_id,updated_at,updated_by,updater_user_id,
                 CASE code
                   WHEN 'CEO' THEN '001' WHEN 'BOARD_CHAIR' THEN '002'
                   WHEN 'BOARD_VICE_CHAIR' THEN '004' WHEN 'BOARD_MEMBER' THEN '006'
                   WHEN 'ALTERNATE_BOARD_MEMBER' THEN '007' WHEN 'PRIMARY_INSPECTOR' THEN '008'
                   WHEN 'ALTERNATE_INSPECTOR' THEN '009' WHEN 'SHAREHOLDER' THEN '020'
                   WHEN 'LEGAL_REPRESENTATIVE' THEN '022' ELSE code END,
                 title FROM related_person_post
                """);
        execute(connection,"ALTER TABLE post_in_company__document_model RENAME TO post_in_company");
    }

    private static void migrateFinalTaxDebtPaper(Connection connection) throws SQLException {
        String table="final_tax_debt_paper";
        if (!tableExists(connection,table)) return;
        addColumn(connection,table,"paper_type_id","INTEGER");
        copyLookup(connection,table,"paper_type_id","paper_type","paper_type","paper_type");
        addColumn(connection,table,"finalization_reason_id","INTEGER");
        copyLookup(connection,table,"finalization_reason_id","finalization_reason","finalization_reason","code");
        addColumn(connection,table,"tax_period_id","INTEGER");
        copyLookup(connection,table,"tax_period_id","tax_period","tax_period","code");
        requireMapped(connection,table,"paper_type","paper_type_id");
        requireMapped(connection,table,"finalization_reason","finalization_reason_id");
        requireMapped(connection,table,"tax_period","tax_period_id");
        boolean legacyColumns=columnExists(connection,table,"paper_type");
        boolean oldMandatoryShape=columnRequired(connection,table,"unique_final_tax_debt_notice_id")
                || columnRequired(connection,table,"final_tax_debt_notice_issue_date");
        if (legacyColumns || oldMandatoryShape || columnExists(connection,table,"tax_source_id")) {
            normalizeFinalizationDocuments(connection);
            rebuildFinalTaxDebtPaper(connection);
        }
    }

    private static void migrateTaxpayer(Connection connection) throws SQLException {
        String table="taxpayer";
        if (!tableExists(connection,table)) return;
        addColumn(connection,table,"follow_code","VARCHAR(255)");
        if (columnExists(connection,table,"registeration_trace_code")) {
            try (var statement=connection.createStatement()) {
                statement.executeUpdate("UPDATE taxpayer SET follow_code=registeration_trace_code WHERE follow_code IS NULL");
            }
        }
        addColumn(connection,table,"taxpayer_type_id","INTEGER");
        copyLookup(connection,table,"taxpayer_type_id","taxpayer_type","taxpayer_type","code");
        requireMapped(connection,table,"registeration_trace_code","follow_code");
        requireMapped(connection,table,"taxpayer_type","taxpayer_type_id");
        if (columnExists(connection,table,"registeration_trace_code")) rebuildTaxpayer(connection);
    }

    private static void migrateRestrictions(Connection connection) throws SQLException {
        String table="restrictions_applied_to_companies";
        if (!tableExists(connection,table)) return;
        addColumn(connection,table,"execution_paper_id","INTEGER");
        if (columnExists(connection,table,"finalized_debt_paper_notification_id")) {
            try (var statement=connection.createStatement()) {
                statement.executeUpdate("UPDATE restrictions_applied_to_companies SET execution_paper_id=CAST(finalized_debt_paper_notification_id AS INTEGER) WHERE execution_paper_id IS NULL");
            }
        }
        addColumn(connection,table,"restriction_state_id","INTEGER");
        if (columnExists(connection,table,"result")) {
            try (var statement=connection.createStatement()) {
                statement.executeUpdate("UPDATE restrictions_applied_to_companies SET restriction_state_id=CASE WHEN result=1 THEN 1 ELSE 0 END WHERE restriction_state_id IS NULL");
            }
        }
        addColumn(connection,table,"restriction_date","TIMESTAMP");
        addColumn(connection,table,"restriction_tracking_code","VARCHAR(255)");
        addColumn(connection,table,"release_date","TIMESTAMP");
        addColumn(connection,table,"release_tracking_code","VARCHAR(255)");
        if (columnExists(connection,table,"finalized_debt_paper_notification_id")) rebuildRestrictions(connection);
    }

    private static void migratePostsInCompany(Connection connection) throws SQLException {
        if (tableExists(connection,"post_in_company")) {
            if (columnExists(connection,"post_in_company","natural_person_allowed")
                    || columnExists(connection,"post_in_company","legal_person_allowed")) {
                execute(connection,"DROP TABLE IF EXISTS post_in_company__document_model");
                execute(connection,"""
                        CREATE TABLE post_in_company__document_model (
                          id INTEGER PRIMARY KEY AUTOINCREMENT,
                          created_at TIMESTAMP NOT NULL, created_by VARCHAR(255) NOT NULL,
                          creator_user_id BIGINT NOT NULL, updated_at TIMESTAMP NOT NULL,
                          updated_by VARCHAR(255) NOT NULL, updater_user_id BIGINT NOT NULL,
                          code_in_company_registration_system VARCHAR(64) NOT NULL UNIQUE,
                          title VARCHAR(255) NOT NULL,
                          FOREIGN KEY(creator_user_id) REFERENCES app_user_profile(id),
                          FOREIGN KEY(updater_user_id) REFERENCES app_user_profile(id))
                        """);
                execute(connection,"""
                        INSERT INTO post_in_company__document_model
                        (id,created_at,created_by,creator_user_id,updated_at,updated_by,updater_user_id,
                         code_in_company_registration_system,title)
                        SELECT id,created_at,created_by,creator_user_id,updated_at,updated_by,updater_user_id,
                         code_in_company_registration_system,title FROM post_in_company
                        """);
                replace(connection,"post_in_company","post_in_company__document_model");
            }
            return;
        }
        String table="related_person_post";
        if (!tableExists(connection,table)) return;
        boolean rebuild = !columnExists(connection,table,"title")
                || !columnExists(connection,table,"natural_person_allowed")
                || !columnExists(connection,table,"legal_person_allowed");
        addColumn(connection,table,"title","VARCHAR(255)");
        addColumn(connection,table,"natural_person_allowed","BOOLEAN");
        addColumn(connection,table,"legal_person_allowed","BOOLEAN");
        try (PreparedStatement statement=connection.prepareStatement(
                "UPDATE related_person_post SET title=?,natural_person_allowed=?,legal_person_allowed=? WHERE code=?")) {
            for (RelatedPersonPostCode value : RelatedPersonPostCode.values()) {
                statement.setString(1,value.label()); statement.setBoolean(2,value.naturalPersonAllowed());
                statement.setBoolean(3,value.legalPersonAllowed()); statement.setString(4,value.name());
                statement.addBatch();
            }
            statement.executeBatch();
        }
        if (rebuild) {
            // Complete the legacy row before it is copied to the document-model table.
            requireMapped(connection,table,"code","title");
        }
        rebuildPostInCompany(connection);
    }

    private static void migrateRelatedPersons(Connection connection) throws SQLException {
        String table="related_persons";
        if (!tableExists(connection,table)) return;
        boolean legacyOwner=columnExists(connection,table,"taxpayer_id");
        boolean legacyPostTarget=foreignKeyReferences(connection,table,"related_person_post");
        boolean extraColumns=columnExists(connection,table,"is_board_member")
                || columnExists(connection,table,"source_system") || columnExists(connection,table,"last_synced_at");
        if (!legacyOwner && !legacyPostTarget && !extraColumns) return;
        normalizeRelatedPersonDates(connection);
        execute(connection,"DROP TABLE IF EXISTS related_persons__document_model");
        execute(connection,"""
                CREATE TABLE related_persons__document_model (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  creation_date TIMESTAMP NOT NULL, created_by VARCHAR(255) NOT NULL,
                  creator_user_id BIGINT NOT NULL, updated_at TIMESTAMP NOT NULL,
                  updated_by VARCHAR(255) NOT NULL, updater_user_id BIGINT NOT NULL,
                  owner_legal_person_id BIGINT NOT NULL, related_natural_person_id BIGINT,
                  related_legal_person_id BIGINT, natural_person_related_to_legal_person_id BIGINT,
                  post_id BIGINT NOT NULL, start_date TIMESTAMP NOT NULL, end_date TIMESTAMP NOT NULL,
                  position_expiry_date TIMESTAMP NOT NULL,
                  CHECK ((related_natural_person_id IS NOT NULL AND related_legal_person_id IS NULL) OR
                         (related_natural_person_id IS NULL AND related_legal_person_id IS NOT NULL)),
                  FOREIGN KEY(creator_user_id) REFERENCES app_user_profile(id),
                  FOREIGN KEY(updater_user_id) REFERENCES app_user_profile(id),
                  FOREIGN KEY(owner_legal_person_id) REFERENCES legal_person(id),
                  FOREIGN KEY(related_natural_person_id) REFERENCES natural_person(id),
                  FOREIGN KEY(related_legal_person_id) REFERENCES legal_person(id),
                  FOREIGN KEY(natural_person_related_to_legal_person_id) REFERENCES natural_person(id),
                  FOREIGN KEY(post_id) REFERENCES post_in_company(id))
                """);
        execute(connection,"""
                INSERT INTO related_persons__document_model
                (id,creation_date,created_by,creator_user_id,updated_at,updated_by,updater_user_id,
                 owner_legal_person_id,related_natural_person_id,related_legal_person_id,
                 natural_person_related_to_legal_person_id,post_id,start_date,end_date,position_expiry_date)
                SELECT id,creation_date,created_by,creator_user_id,updated_at,updated_by,updater_user_id,
                 owner_legal_person_id,related_natural_person_id,related_legal_person_id,
                 natural_person_related_to_legal_person_id,post_id,start_date,end_date,position_expiry_date
                 FROM related_persons
                """);
        replace(connection,"related_persons","related_persons__document_model");
    }

    private static void normalizeRelatedPersonDates(Connection connection) throws SQLException {
        try (var query=connection.createStatement();var rows=query.executeQuery(
                "SELECT id,start_date,end_date,position_expiry_date FROM related_persons");
             var update=connection.prepareStatement(
                     "UPDATE related_persons SET start_date=?,end_date=?,position_expiry_date=? WHERE id=?")) {
            while(rows.next()) {
                update.setString(1,normalizeDate(rows.getString(2)));
                update.setString(2,normalizeDate(rows.getString(3)));
                update.setString(3,normalizeDate(rows.getString(4)));
                update.setLong(4,rows.getLong(1)); update.addBatch();
            }
            update.executeBatch();
        }
    }

    private static String normalizeDate(String value) {
        if (value==null || value.isBlank()) return value;
        String normalized=value.trim().replace('/','-');
        int year=Integer.parseInt(normalized.substring(0,4));
        if (year>=1700) return normalized.length()==10 ? normalized+"T00:00:00" : normalized.replace(' ','T');
        return com.mparsict.vosolmaliat.domain.value.PersianDates.parse(normalized).toString();
    }

    private static void normalizeFinalizationDocuments(Connection connection) throws SQLException {
        execute(connection,"""
                UPDATE final_tax_debt_paper
                SET tax_return_number=COALESCE(tax_return_number,unique_final_tax_debt_notice_id),
                    tax_return_date=COALESCE(tax_return_date,final_tax_debt_notice_issue_date),
                    unique_final_tax_debt_notice_id=NULL,
                    final_tax_debt_notice_issue_date=NULL
                WHERE finalization_reason_id=0
                """);
        execute(connection,"""
                UPDATE final_tax_debt_paper
                SET unique_final_tax_debt_notice_id=COALESCE(unique_final_tax_debt_notice_id,tax_return_number),
                    final_tax_debt_notice_issue_date=COALESCE(final_tax_debt_notice_issue_date,tax_return_date),
                    tax_return_number=NULL,
                    tax_return_date=NULL
                WHERE finalization_reason_id=1
                """);
    }

    private static void rebuildFinalTaxDebtPaper(Connection connection) throws SQLException {
        execute(connection,"DROP TABLE IF EXISTS final_tax_debt_paper__document_model");
        execute(connection,"""
                CREATE TABLE final_tax_debt_paper__document_model (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  created_at TIMESTAMP NOT NULL, created_by VARCHAR(255) NOT NULL,
                  creator_user_id BIGINT NOT NULL, updated_at TIMESTAMP NOT NULL,
                  updated_by VARCHAR(255) NOT NULL, updater_user_id BIGINT NOT NULL,
                  paper_type_id BIGINT NOT NULL, unique_final_tax_debt_notice_id VARCHAR(255) UNIQUE,
                  tax_return_number VARCHAR(255), final_tax_debt_notice_issue_date TIMESTAMP,
                  tax_return_date TIMESTAMP, final_tax_debt_paper_notification_number VARCHAR(255) NOT NULL,
                  final_tax_debt_paper_notification_view_date TIMESTAMP, taxpayer_id BIGINT NOT NULL,
                  finalization_reason_id INTEGER NOT NULL,
                  tax_year INTEGER NOT NULL, tax_period_id INTEGER, period_start_date TIMESTAMP NOT NULL,
                  period_end_date TIMESTAMP NOT NULL, tax_affairs_office_id BIGINT NOT NULL,
                  FOREIGN KEY(creator_user_id) REFERENCES app_user_profile(id),
                  FOREIGN KEY(updater_user_id) REFERENCES app_user_profile(id),
                  FOREIGN KEY(paper_type_id) REFERENCES paper_type(id),
                  FOREIGN KEY(finalization_reason_id) REFERENCES finalization_reason(id),
                  FOREIGN KEY(tax_period_id) REFERENCES tax_period(id),
                  FOREIGN KEY(taxpayer_id) REFERENCES taxpayer(id),
                  FOREIGN KEY(tax_affairs_office_id) REFERENCES tax_affairs_office(id),
                  CHECK ((finalization_reason_id=0 AND tax_return_number IS NOT NULL AND tax_return_date IS NOT NULL
                           AND unique_final_tax_debt_notice_id IS NULL AND final_tax_debt_notice_issue_date IS NULL)
                      OR (finalization_reason_id=1 AND unique_final_tax_debt_notice_id IS NOT NULL
                           AND final_tax_debt_notice_issue_date IS NOT NULL AND tax_return_number IS NULL
                           AND tax_return_date IS NULL)))
                """);
        execute(connection,"""
                INSERT INTO final_tax_debt_paper__document_model
                (id,created_at,created_by,creator_user_id,updated_at,updated_by,updater_user_id,
                 paper_type_id,unique_final_tax_debt_notice_id,tax_return_number,final_tax_debt_notice_issue_date,
                 tax_return_date,final_tax_debt_paper_notification_number,final_tax_debt_paper_notification_view_date,
                 taxpayer_id,finalization_reason_id,tax_year,tax_period_id,period_start_date,
                 period_end_date,tax_affairs_office_id)
                SELECT id,created_at,created_by,creator_user_id,updated_at,updated_by,updater_user_id,
                 paper_type_id,unique_final_tax_debt_notice_id,tax_return_number,final_tax_debt_notice_issue_date,
                 tax_return_date,final_tax_debt_paper_notification_number,final_tax_debt_paper_notification_view_date,
                 taxpayer_id,finalization_reason_id,tax_year,tax_period_id,period_start_date,
                 period_end_date,tax_affairs_office_id FROM final_tax_debt_paper
                """);
        replace(connection,"final_tax_debt_paper","final_tax_debt_paper__document_model");
    }

    private static void rebuildTaxpayer(Connection connection) throws SQLException {
        execute(connection,"DROP TABLE IF EXISTS taxpayer__document_model");
        execute(connection,"""
                CREATE TABLE taxpayer__document_model (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  creation_date TIMESTAMP NOT NULL, created_by VARCHAR(255) NOT NULL,
                  creator_user_id BIGINT NOT NULL, updated_at TIMESTAMP NOT NULL,
                  updated_by VARCHAR(255) NOT NULL, updater_user_id BIGINT NOT NULL,
                  follow_code VARCHAR(255) NOT NULL, taxpayer_id VARCHAR(255) NOT NULL UNIQUE,
                  natural_person_id BIGINT, legal_person_id BIGINT UNIQUE, taxpayer_type_id INTEGER NOT NULL,
                  CHECK ((natural_person_id IS NOT NULL AND legal_person_id IS NULL) OR
                         (natural_person_id IS NULL AND legal_person_id IS NOT NULL)),
                  FOREIGN KEY(creator_user_id) REFERENCES app_user_profile(id),
                  FOREIGN KEY(updater_user_id) REFERENCES app_user_profile(id),
                  FOREIGN KEY(natural_person_id) REFERENCES natural_person(id),
                  FOREIGN KEY(legal_person_id) REFERENCES legal_person(id),
                  FOREIGN KEY(taxpayer_type_id) REFERENCES taxpayer_type(id))
                """);
        execute(connection,"""
                INSERT INTO taxpayer__document_model
                (id,creation_date,created_by,creator_user_id,updated_at,updated_by,updater_user_id,
                 follow_code,taxpayer_id,natural_person_id,legal_person_id,taxpayer_type_id)
                SELECT id,creation_date,created_by,creator_user_id,updated_at,updated_by,updater_user_id,
                 follow_code,taxpayer_id,natural_person_id,legal_person_id,taxpayer_type_id FROM taxpayer
                """);
        replace(connection,"taxpayer","taxpayer__document_model");
    }

    private static void rebuildRestrictions(Connection connection) throws SQLException {
        execute(connection,"DROP TABLE IF EXISTS restrictions_applied_to_companies__document_model");
        execute(connection,"""
                CREATE TABLE restrictions_applied_to_companies__document_model (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  creation_date TIMESTAMP NOT NULL, created_by VARCHAR(255) NOT NULL,
                  creator_user_id BIGINT NOT NULL, updated_at TIMESTAMP NOT NULL,
                  updated_by VARCHAR(255) NOT NULL, updater_user_id BIGINT NOT NULL,
                  execution_paper_id BIGINT NOT NULL, natural_person_id BIGINT NOT NULL,
                  restriction_state_id INTEGER NOT NULL, restriction_date TIMESTAMP,
                  restriction_tracking_code VARCHAR(255) UNIQUE, release_date TIMESTAMP,
                  release_tracking_code VARCHAR(255) UNIQUE,
                  FOREIGN KEY(creator_user_id) REFERENCES app_user_profile(id),
                  FOREIGN KEY(updater_user_id) REFERENCES app_user_profile(id),
                  FOREIGN KEY(execution_paper_id) REFERENCES execution_paper(id),
                  FOREIGN KEY(natural_person_id) REFERENCES natural_person(id),
                  FOREIGN KEY(restriction_state_id) REFERENCES restriction_state(id))
                """);
        execute(connection,"""
                INSERT INTO restrictions_applied_to_companies__document_model
                (id,creation_date,created_by,creator_user_id,updated_at,updated_by,updater_user_id,
                 execution_paper_id,natural_person_id,restriction_state_id,restriction_date,
                 restriction_tracking_code,release_date,release_tracking_code)
                SELECT id,creation_date,created_by,creator_user_id,updated_at,updated_by,updater_user_id,
                 execution_paper_id,natural_person_id,restriction_state_id,restriction_date,
                 restriction_tracking_code,release_date,release_tracking_code
                 FROM restrictions_applied_to_companies
                """);
        replace(connection,"restrictions_applied_to_companies","restrictions_applied_to_companies__document_model");
    }

    private static void replace(Connection connection,String target,String temporary) throws SQLException {
        execute(connection,"DROP TABLE " + target);
        execute(connection,"ALTER TABLE " + temporary + " RENAME TO " + target);
    }

    private static void execute(Connection connection,String sql) throws SQLException {
        try(var statement=connection.createStatement()){statement.execute(sql);}
    }

    private static void copyLookup(Connection connection,String owner,String target,String source,String lookup,String code) throws SQLException {
        if (!columnExists(connection,owner,source)) return;
        try (var statement=connection.createStatement()) {
            statement.executeUpdate("UPDATE " + owner + " SET " + target + "=(SELECT id FROM " + lookup
                    + " WHERE " + lookup + "." + code + "=CAST(" + owner + "." + source + " AS VARCHAR)) WHERE " + target + " IS NULL");
        }
    }

    private static void addColumn(Connection connection,String table,String column,String definition) throws SQLException {
        if (columnExists(connection,table,column)) return;
        try (var statement=connection.createStatement()) { statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition); }
    }

    private static void requireMapped(Connection connection,String table,String source,String target) throws SQLException {
        if (!columnExists(connection,table,source)) return;
        try (var statement=connection.createStatement();
             var rows=statement.executeQuery("SELECT COUNT(*) FROM " + table + " WHERE " + source + " IS NOT NULL AND " + target + " IS NULL")) {
            if (rows.next() && rows.getLong(1)>0)
                throw new SQLException("Cannot migrate " + table + "." + source + " to " + target + "; unknown lookup value exists");
        }
    }

    private static boolean tableExists(Connection connection,String table) throws SQLException {
        try (PreparedStatement statement=connection.prepareStatement("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?")) {
            statement.setString(1,table); try(ResultSet rows=statement.executeQuery()){return rows.next();}
        }
    }

    private static boolean columnExists(Connection connection,String table,String column) throws SQLException {
        try (var statement=connection.createStatement();var rows=statement.executeQuery("PRAGMA table_info(\""+table+"\")")) {
            while(rows.next()) if(column.equalsIgnoreCase(rows.getString("name"))) return true;
            return false;
        }
    }

    private static boolean columnRequired(Connection connection,String table,String column) throws SQLException {
        try (var statement=connection.createStatement();var rows=statement.executeQuery("PRAGMA table_info(\""+table+"\")")) {
            while(rows.next()) if(column.equalsIgnoreCase(rows.getString("name"))) return rows.getInt("notnull") != 0;
            return false;
        }
    }

    private static boolean foreignKeyReferences(Connection connection,String table,String target) throws SQLException {
        try (var statement=connection.createStatement();var rows=statement.executeQuery("PRAGMA foreign_key_list(\""+table+"\")")) {
            while(rows.next()) if(target.equalsIgnoreCase(rows.getString("table"))) return true;
            return false;
        }
    }

    private static int pragma(Connection connection,String name) throws SQLException {
        try(var statement=connection.createStatement();var rows=statement.executeQuery("PRAGMA "+name)) {
            return rows.next()?rows.getInt(1):0;
        }
    }
}
