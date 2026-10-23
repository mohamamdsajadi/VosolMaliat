package com.mparsict.vosolmaliat.common.config;

import com.mparsict.vosolmaliat.domain.enums.BusinessRole;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

/** Replaces the old enum CHECK without changing profile IDs or audit references. */
public final class SqliteBusinessRoleMigration {
    private SqliteBusinessRoleMigration() {}
    public static void migrate(Connection connection) throws SQLException {
        String definition;
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(
                "SELECT sql FROM sqlite_master WHERE type='table' AND name='app_user_profile'")) {
            if (!rows.next()) return;
            definition = rows.getString(1);
        }
        if (!definition.contains("GENERAL_OFFICE_HEAD") || definition.contains("TAX_COLLECTION_OFFICE")) return;
        String roles = Arrays.stream(BusinessRole.values()).map(role -> "'" + role.name() + "'").collect(Collectors.joining(","));
        // Keep old values readable during the one-time bootstrap data migration.
        roles += ",'GENERAL_OFFICE_HEAD','TAX_OFFICE_HEAD','TAX_EXPERT'";
        String replacement = definition.replaceFirst("(?i)CREATE TABLE\\s+[\"`]?app_user_profile[\"`]?", "CREATE TABLE app_user_profile__roles")
                .replaceFirst("(?i)check\\s*\\(\\s*business_role\\s+in\\s*\\([^)]*\\)\\s*\\)", "CHECK (business_role IN (" + roles + "))");
        var indexes = new ArrayList<String>();
        try (var statement = connection.createStatement(); var rows = statement.executeQuery(
                "SELECT sql FROM sqlite_master WHERE tbl_name='app_user_profile' AND type IN ('index','trigger') AND sql IS NOT NULL")) {
            while (rows.next()) indexes.add(rows.getString(1));
        }
        boolean foreignKeys;
        try (var statement = connection.createStatement(); var result = statement.executeQuery("PRAGMA foreign_keys")) { result.next(); foreignKeys = result.getInt(1) != 0; }
        try (var statement = connection.createStatement()) { statement.execute("PRAGMA foreign_keys=OFF"); }
        connection.setAutoCommit(false);
        try (var statement = connection.createStatement()) {
            statement.execute(replacement);
            statement.execute("INSERT INTO app_user_profile__roles SELECT * FROM app_user_profile");
            statement.execute("DROP TABLE app_user_profile");
            statement.execute("ALTER TABLE app_user_profile__roles RENAME TO app_user_profile");
            for (String sql : indexes) statement.execute(sql);
            try (var violations = statement.executeQuery("PRAGMA foreign_key_check")) {
                if (violations.next()) throw new SQLException("Role migration would break a foreign key");
            }
            connection.commit();
        } catch (SQLException failure) { connection.rollback(); throw failure; }
        finally {
            connection.setAutoCommit(true);
            try (var statement = connection.createStatement()) { statement.execute("PRAGMA foreign_keys=" + (foreignKeys ? "ON" : "OFF")); }
        }
    }
}
