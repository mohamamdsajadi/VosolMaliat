package com.mparsict.vosolmaliat.unit.service;

import com.mparsict.vosolmaliat.application.dto.response.DatabaseResetResponse;
import com.mparsict.vosolmaliat.application.service.DatabaseResetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseResetServiceUnitTest {

    @TempDir
    Path tempDirectory;

    @Test
    void resetClearsAllApplicationTablesAndRestartsIdentitySequences() throws Exception {
        DataSource dataSource = dataSource();
        execute(dataSource, "PRAGMA foreign_keys = ON");
        execute(dataSource, "CREATE TABLE parent (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL)");
        execute(dataSource, "CREATE TABLE child (id INTEGER PRIMARY KEY AUTOINCREMENT, parent_id INTEGER NOT NULL REFERENCES parent(id))");
        execute(dataSource, "INSERT INTO parent(name) VALUES ('parent')");
        execute(dataSource, "INSERT INTO child(parent_id) VALUES (1)");

        DatabaseResetResponse response = new DatabaseResetService(dataSource).reset();

        assertThat(response.tablesCleared()).isEqualTo(2);
        assertThat(response.rowsDeleted()).isEqualTo(2);
        assertThat(count(dataSource, "parent")).isZero();
        assertThat(count(dataSource, "child")).isZero();

        execute(dataSource, "INSERT INTO parent(name) VALUES ('new parent')");
        assertThat(singleLong(dataSource, "SELECT id FROM parent")).isEqualTo(1L);
        assertThat(singleLong(dataSource, "PRAGMA foreign_keys")).isEqualTo(1L);
    }

    private DataSource dataSource() {
        return new SingleConnectionDataSource(
                "jdbc:sqlite:" + tempDirectory.resolve("reset-test.sqlite"),
                true
        );
    }

    private void execute(DataSource dataSource, String sql) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long count(DataSource dataSource, String table) throws Exception {
        return singleLong(dataSource, "SELECT COUNT(*) FROM " + table);
    }

    private long singleLong(DataSource dataSource, String sql) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }
}
