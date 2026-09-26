package com.mparsict.vosolmaliat;

import com.mparsict.vosolmaliat.common.config.SqliteLegacySchemaMigrator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VosolMaliatApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(VosolMaliatApplication.class);
        application.addInitializers(new SqliteLegacySchemaMigrator());
        application.run(args);
    }
}
