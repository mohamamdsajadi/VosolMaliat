package com.mparsict.vosolmaliat.common.config;

import com.mparsict.vosolmaliat.application.service.Ef03SampleImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(name = "app.ef03.import-samples", havingValue = "true")
public class Ef03SampleImportRunner implements ApplicationRunner {
    private final Ef03SampleImportService importer;

    @Override
    public void run(ApplicationArguments args) {
        var report = importer.importAll();
        log.info("EF03 sample import complete: total={}, imported={}, skipped={}, failed={}",
                report.total(), report.imported(), report.skipped(), report.failed());
        report.failures().forEach(failure -> log.error("EF03 sample import failure: {}", failure));
        if (report.failed() > 0) {
            throw new IllegalStateException("EF03 sample import failed for " + report.failed() + " row(s)");
        }
    }
}
