package com.mparsict.vosolmaliat.application.service.arrest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.common.util.UniqueCodeGenerator;
import com.mparsict.vosolmaliat.domain.repository.ArrestOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Locale;

@Component @RequiredArgsConstructor
public class ArrestOrderNumbers {
    private final ArrestOrderRepository orders;
    private final ObjectMapper json;

    static String number(Long id, Instant issuedAt) {
        return String.format(Locale.ROOT, "%d%010d", UniqueCodeGenerator.persianYear(
                issuedAt.atZone(ZoneId.of("Asia/Tehran")).toLocalDate()), id);
    }

    /** Upgrade legacy demo letters and their saved print snapshots together. Idempotent. */
    @EventListener(ApplicationReadyEvent.class) @Transactional
    public void migrateLegacyNumbers() {
        for (var order : orders.findAll()) {
            if (order.getLetterNumber() == null || !order.getLetterNumber().matches("AO-\\d+-\\d+")) continue;
            var number = number(order.getId(), order.getIssuedAt());
            try {
                var snapshot = json.readValue(order.getLetterSnapshot(), new TypeReference<LinkedHashMap<String, Object>>() {});
                snapshot.put("letterNumber", number);
                order.setLetterNumber(number);
                order.setLetterSnapshot(json.writeValueAsString(snapshot));
            } catch (Exception ex) { throw new IllegalStateException("Cannot migrate arrest letter number", ex); }
        }
    }
}
