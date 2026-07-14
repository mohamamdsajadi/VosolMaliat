package com.mparsict.vosolmaliat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VosolMaliatApplication {
    public static void main(String[] args) {
        SpringApplication.run(VosolMaliatApplication.class, args);
    }
}
