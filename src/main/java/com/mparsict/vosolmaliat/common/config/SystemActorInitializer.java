package com.mparsict.vosolmaliat.common.config;

import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.sql.*;

@Component @RequiredArgsConstructor @Order(Ordered.HIGHEST_PRECEDENCE)
public class SystemActorInitializer implements ApplicationRunner {
    private final DataSource dataSource;
    @Override public void run(ApplicationArguments args) throws Exception {
        try (var connection=dataSource.getConnection()) { seed(connection); }
    }
    public static void seed(Connection connection) throws SQLException {
        try(var statement=connection.createStatement()) {
            statement.executeUpdate("""
                INSERT INTO app_user_profile
                    (id,keycloak_user_id,username,enabled_in_business_system,locked_in_business_system,
                     created_at,updated_at,created_by,updated_by,creator_user_id,updater_user_id,is_deleted)
                VALUES (0,'SYSTEM','SYSTEM',0,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'SYSTEM','SYSTEM',0,0,0)
                ON CONFLICT(id) DO NOTHING
                """);
        }
    }
}
