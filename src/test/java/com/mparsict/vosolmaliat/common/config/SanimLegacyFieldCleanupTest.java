package com.mparsict.vosolmaliat.common.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class SanimLegacyFieldCleanupTest {
    @TempDir Path directory;
    @Test void removesAllThirtyScalarsPreservingEveryForeignKeyAndExactNewData() throws Exception {
        String url = "jdbc:sqlite:" + directory.resolve("existing.sqlite");
        Map<String,List<String>> before = new HashMap<>();
        try (var c = DriverManager.getConnection(url); var s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys=ON");
            s.execute("CREATE TABLE tax_unit(id INTEGER PRIMARY KEY)");
            s.execute("CREATE TABLE taxpayer(id INTEGER PRIMARY KEY)");
            s.execute("INSERT INTO tax_unit VALUES(7)"); s.execute("INSERT INTO taxpayer VALUES(2)");
            s.execute("CREATE TABLE finalized_debt_paper(id INTEGER PRIMARY KEY, tax_unit_id INTEGER REFERENCES tax_unit(id))");
            s.execute("CREATE TABLE execution_paper(id INTEGER PRIMARY KEY, finalized_debt_paper_id INTEGER REFERENCES finalized_debt_paper(id), taxpayer_id INTEGER REFERENCES taxpayer(id), tax_unit_id INTEGER REFERENCES tax_unit(id))");
            s.execute("CREATE TABLE debt_information(id INTEGER PRIMARY KEY, execution_paper_id INTEGER REFERENCES execution_paper(id))");
            s.execute("CREATE TABLE debt_paper_push_record(id INTEGER PRIMARY KEY, reference_id TEXT UNIQUE, source_document_id INTEGER REFERENCES finalized_debt_paper(id), execution_paper_id INTEGER REFERENCES execution_paper(id), xml_payload TEXT)");
            s.execute("CREATE TABLE debt_inquiry_result(id INTEGER PRIMARY KEY, execution_paper_id INTEGER REFERENCES execution_paper(id), request_id TEXT UNIQUE, response_json TEXT)");
            for (var entry : SanimLegacyFieldCleanup.COLUMNS.entrySet()) {
                if (!tableExists(c, entry.getKey())) {
                    continue;
                }
                Set<String> existingColumns = columns(c, entry.getKey());
                for (String column : entry.getValue()) {
                    if (!existingColumns.contains(column)) {
                        s.execute("ALTER TABLE " + entry.getKey() + " ADD COLUMN " + column + " TEXT");
                    }
                }
                before.put(entry.getKey(), foreignKeys(c, entry.getKey()));
            }
            SanimDocumentSchemaMigration.migrate(c);
            s.execute("INSERT INTO finalized_debt_paper(id,tax_unit_id,source_document_number,source_issue_date_raw,aggregate_outstanding_debt_amount,performance_year) VALUES(10,7,'doc','1400/01/01','-999999999999999999.123456','1400')");
            s.execute("INSERT INTO execution_paper(id,finalized_debt_paper_id,taxpayer_id,tax_unit_id) VALUES(20,10,2,7)");
            s.execute("INSERT INTO debt_information(id,execution_paper_id,aggregate_outstanding_debt_amount,tax_amount) VALUES(30,20,'-999999999999999999.123456','12.5')");
            s.execute("INSERT INTO debt_paper_push_record(id,reference_id,source_document_id,execution_paper_id,xml_payload,source_tax_office_code,tax_office_id) VALUES(40,'ref',10,20,'<raw>  original </raw>','00123','123')");
            s.execute("INSERT INTO debt_inquiry_result(id,execution_paper_id,request_id,response_json) VALUES(50,20,'request','{}')");
        }
        SqliteLegacySchemaMigrator.migrate(url);
        SqliteLegacySchemaMigrator.migrate(url);
        try (var c = DriverManager.getConnection(url); var s = c.createStatement()) {
            for (var entry : SanimLegacyFieldCleanup.COLUMNS.entrySet()) {
                if (!tableExists(c, entry.getKey())) {
                    continue;
                }
                List<String> retainedForeignKeys = before.get(entry.getKey()).stream()
                        .filter(key -> entry.getValue().stream()
                                .noneMatch(column -> key.contains(":" + column + ":")))
                        .toList();
                assertThat(foreignKeys(c,entry.getKey())).isEqualTo(retainedForeignKeys);
                var columns = columns(c, entry.getKey());
                assertThat(columns).doesNotContainAnyElementsOf(entry.getValue());
            }
            try(var row=s.executeQuery("SELECT * FROM debt_information WHERE id=30")) {
                assertThat(row.next()).isTrue();
                assertThat(row.getLong("execution_paper_id")).isEqualTo(20);
                assertThat(row.getString("aggregate_outstanding_debt_amount")).isEqualTo("-999999999999999999.123456");
            }
            try(var row=s.executeQuery("SELECT * FROM debt_paper_push_record WHERE id=40")) {
                assertThat(row.next()).isTrue();
                assertThat(row.getString("xml_payload")).isEqualTo("<raw>  original </raw>");
                assertThat(row.getString("source_tax_office_code")).isEqualTo("00123");
                assertThat(row.getLong("source_document_id")).isEqualTo(10);
            }
            try(var row=s.executeQuery("PRAGMA foreign_key_check")) { assertThat(row.next()).isFalse(); }
            try(var backups=Files.list(directory)) { assertThat(backups.filter(p->p.toString().contains("pre-sanim-cleanup")).count()).isEqualTo(1); }
        }
    }
    @Test void incompatibleTriggerRollsBackColumnRemovalAndBackupRetainsOriginal() throws Exception {
        String url = "jdbc:sqlite:" + directory.resolve("trigger.sqlite");
        try(var c=DriverManager.getConnection(url);var s=c.createStatement()) {
            s.execute("CREATE TABLE debt_information(id INTEGER PRIMARY KEY, tax_amount REAL, duty_amount REAL)");
            s.execute("INSERT INTO debt_information VALUES(1,12.5,3)");
            s.execute("CREATE TRIGGER legacy_trigger AFTER UPDATE ON debt_information BEGIN SELECT NEW.tax_amount; END");
        }
        assertThatThrownBy(()->SqliteLegacySchemaMigrator.migrate(url)).isInstanceOf(IllegalStateException.class);
        try(var c=DriverManager.getConnection(url);var s=c.createStatement();var row=s.executeQuery("SELECT tax_amount,duty_amount FROM debt_information")) {
            assertThat(row.next()).isTrue(); assertThat(row.getDouble(1)).isEqualTo(12.5); assertThat(row.getDouble(2)).isEqualTo(3);
        }
        try(var backups=Files.list(directory)) { assertThat(backups.filter(p->p.toString().contains("pre-sanim-cleanup")).count()).isEqualTo(1); }
    }
    private Set<String> columns(Connection c,String table) throws Exception {
        Set<String> result=new HashSet<>();
        try(var s=c.createStatement();var rows=s.executeQuery("PRAGMA table_info("+table+")")) { while(rows.next())result.add(rows.getString("name")); }
        return result;
    }
    private boolean tableExists(Connection c, String table) throws Exception {
        try (var statement = c.prepareStatement("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?")) {
            statement.setString(1, table);
            try (var rows = statement.executeQuery()) { return rows.next(); }
        }
    }
    private List<String> foreignKeys(Connection c,String table) throws Exception {
        List<String> result=new ArrayList<>();
        try(var s=c.createStatement();var rows=s.executeQuery("PRAGMA foreign_key_list("+table+")")) {
            while(rows.next())result.add(rows.getString("table")+":"+rows.getString("from")+":"+rows.getString("to")+":"+rows.getString("on_delete"));
        }
        return result;
    }
}
