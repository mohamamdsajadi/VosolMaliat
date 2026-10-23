package com.mparsict.vosolmaliat.unit.config;

import com.mparsict.vosolmaliat.common.config.SqliteBusinessRoleMigration;
import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SqliteBusinessRoleMigrationTest {
    @Test
    void widensRoleConstraintAndPreservesRowsReferencesIndexesAndIdempotence() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:"); var sql = connection.createStatement()) {
            sql.execute("PRAGMA foreign_keys=ON");
            sql.execute("CREATE TABLE app_user_profile(id INTEGER PRIMARY KEY, username TEXT, business_role TEXT CHECK (business_role IN ('GENERAL_OFFICE_HEAD','TAX_OFFICE_HEAD','TAX_EXPERT','ADMIN')))");
            sql.execute("CREATE UNIQUE INDEX user_name ON app_user_profile(username)");
            sql.execute("INSERT INTO app_user_profile VALUES(1,'old-sample','GENERAL_OFFICE_HEAD')");
            sql.execute("CREATE TABLE audit(actor INTEGER REFERENCES app_user_profile(id))");
            sql.execute("INSERT INTO audit VALUES(1)");
            SqliteBusinessRoleMigration.migrate(connection);
            SqliteBusinessRoleMigration.migrate(connection);
            sql.execute("INSERT INTO app_user_profile VALUES(2,'new-demo','EXECUTION_OFFICER')");
            try (var rows = sql.executeQuery("SELECT actor FROM audit JOIN app_user_profile ON actor=id")) {
                assertThat(rows.next()).isTrue(); assertThat(rows.getInt(1)).isEqualTo(1);
            }
            try (var rows = sql.executeQuery("PRAGMA foreign_key_check")) { assertThat(rows.next()).isFalse(); }
            try (var rows = sql.executeQuery("SELECT count(*) FROM sqlite_master WHERE name='user_name'")) {
                rows.next(); assertThat(rows.getInt(1)).isEqualTo(1);
            }
        }
    }
}
