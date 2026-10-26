package com.mparsict.vosolmaliat.common.config;

import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import java.sql.SQLException;
import static org.assertj.core.api.Assertions.*;

class SqliteExecutionPaperIssuerMigrationTest {
    @Test
    void upgradesExistingPapersAndDefaultsFutureSqlInsertsWithoutChangingOtherData() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE execution_paper (id INTEGER PRIMARY KEY, execution_paper_unique_code TEXT)");
            statement.execute("INSERT INTO execution_paper VALUES (1,'OLD-1'),(2,'OLD-2')");
            SqliteExecutionPaperIssuerMigration.migrate(connection);
            statement.execute("INSERT INTO execution_paper (id,execution_paper_unique_code) VALUES (3,'NEW-3')");
            try (var rows = statement.executeQuery("SELECT * FROM execution_paper ORDER BY id")) {
                for (int id = 1; id <= 3; id++) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getInt("id")).isEqualTo(id);
                    assertThat(rows.getString("execution_paper_unique_code")).isEqualTo((id == 3 ? "NEW-" : "OLD-") + id);
                    assertThat(rows.getString("issuer_system")).isEqualTo("SANIM");
                }
                assertThat(rows.next()).isFalse();
            }
            statement.execute("UPDATE execution_paper SET issuer_system='TABRIZ' WHERE id=2");
            SqliteExecutionPaperIssuerMigration.migrate(connection);
            try (var rows = statement.executeQuery("SELECT issuer_system FROM execution_paper WHERE id=2")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("TABRIZ");
            }
            assertThatThrownBy(() -> statement.execute("UPDATE execution_paper SET issuer_system='OTHER' WHERE id=1"))
                    .isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> statement.execute("UPDATE execution_paper SET issuer_system=NULL WHERE id=1"))
                    .isInstanceOf(SQLException.class);
        }
    }

    @Test
    void repairsMissingValuesInAnExistingColumnAndPreservesExplicitIssuers() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE execution_paper (id INTEGER PRIMARY KEY, issuer_system TEXT)");
            statement.execute("INSERT INTO execution_paper VALUES (1,NULL),(2,'  '),(3,'TABRIZ')");
            SqliteExecutionPaperIssuerMigration.migrate(connection);
            try (var rows = statement.executeQuery("SELECT group_concat(issuer_system,',') FROM (SELECT issuer_system FROM execution_paper ORDER BY id)")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("SANIM,SANIM,TABRIZ");
            }
        }
    }

    @Test
    void leavesFreshDatabaseCreationToHibernate() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            SqliteExecutionPaperIssuerMigration.migrate(connection);
            try (var statement = connection.createStatement();
                 var tables = statement.executeQuery("SELECT count(*) FROM sqlite_master WHERE type='table'")) {
                assertThat(tables.next()).isTrue();
                assertThat(tables.getInt(1)).isZero();
            }
        }
    }
}
