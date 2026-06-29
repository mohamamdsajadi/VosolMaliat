package com.mparsict.vosolmaliat.common.util;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UniqueCodeGenerator {

    /**
     * Generates a unique execution paper code in the format EP-{timestamp}-{random6hex}.
     */
    public String generateExecutionPaperCode() {
        long timestamp = System.currentTimeMillis();
        String random6hex = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        return "EP-" + timestamp + "-" + random6hex;
    }

    /**
     * Computes the SHA-256 hash of the given input and returns it as a hex string.
     */
    public String generateHash(String input) {
        return HashUtil.sha256(input);
    }
}
