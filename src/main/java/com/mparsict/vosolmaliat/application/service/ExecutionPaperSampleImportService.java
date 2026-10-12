package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.domain.enums.DebtPaperProcessingStatus;
import com.mparsict.vosolmaliat.domain.enums.PaperType;
import com.mparsict.vosolmaliat.domain.model.masterdata.ActivityType;
import com.mparsict.vosolmaliat.domain.model.masterdata.GeneralDirectorateOfTaxAffairs;
import com.mparsict.vosolmaliat.domain.model.masterdata.TaxAffairsOffice;
import com.mparsict.vosolmaliat.domain.model.masterdata.TaxAffairsOfficeHistory;
import com.mparsict.vosolmaliat.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static com.mparsict.vosolmaliat.application.service.DebtPaperXmlField.*;

/** Idempotent, row-isolated importer for explicitly selected workbook samples. */
@Service
@RequiredArgsConstructor
public class ExecutionPaperSampleImportService {
    private final ExecutionPaperSampleService samples;
    private final ExecutionPaperLenientImportMapper mapper;
    private final DebtPaperPushPersistenceService persistence;
    private final ExecutionPaperService executionPapers;
    private final DebtPaperPushRecordRepository pushRecords;
    private final DebtPaperXmlReader xmlReader;
    private final GeneralDirectorateOfTaxAffairsRepository generalDirectorates;
    private final TaxAffairsOfficeRepository taxOffices;
    private final TaxAffairsOfficeHistoryRepository taxOfficeHistory;
    private final ActivityTypeRepository activityTypes;

    public ImportReport importSelected(Set<String> referenceIds) {
        if (referenceIds == null || referenceIds.isEmpty()) {
            throw new IllegalArgumentException("At least one reference id must be selected.");
        }
        List<ExecutionPaperSampleService.Sample> selected = samples.samples(Set.of(), referenceIds);
        Set<String> found = new HashSet<>();
        selected.forEach(sample -> found.add(sample.referenceId()));
        List<String> failures = new ArrayList<>();
        referenceIds.stream().filter(reference -> !found.contains(reference))
                .forEach(reference -> failures.add(reference + ": sample was not found"));
        ImportReport processed = importSamples(selected, failures, referenceIds.size());
        return processed;
    }

    public ImportReport importWorkbook(Path workbook) {
        var read = samples.readWorkbook(workbook);
        return importSamples(read.samples(), new ArrayList<>(read.failures()), read.totalRows());
    }

    private ImportReport importSamples(List<ExecutionPaperSampleService.Sample> selected,
            List<String> failures, int requested) {
        int imported = 0;
        int skipped = 0;
        OffsetDateTime receivedAt = OffsetDateTime.now(ZoneOffset.UTC);
        for (var sample : selected) {
            Long recordId = null;
            try {
                var existing = pushRecords.findByReferenceId(sample.referenceId());
                if (existing.isPresent() && existing.get().getProcessingStatus() == DebtPaperProcessingStatus.SUCCESS) {
                    skipped++;
                    continue;
                }
                var input = sample.request(receivedAt);
                recordId = existing.isPresent()
                        ? persistence.resumeSampleImport(existing.get().getId())
                        : persistence.receive(input, "execution-paper-sample-import-" + sample.referenceId());
                var request = mapper.map(input);
                ensureReferenceData(request.sourceDocument(), sample.xml());
                persistence.prepareImportWorkflow(recordId, request);
                var executionPaper = executionPapers.createFromImportRequest(request);
                persistence.markSuccessful(recordId, executionPaper.getId());
                imported++;
            } catch (RuntimeException exception) {
                if (recordId != null) persistence.markNeedsReview(recordId, message(exception));
                failures.add(sample.referenceId() + ": " + message(exception));
            }
        }
        return new ImportReport(requested, imported, skipped, List.copyOf(failures));
    }

    private void ensureReferenceData(
            com.mparsict.vosolmaliat.application.dto.request.SanimDocumentDetails details,
            String sourceXml) {
        var document = xmlReader.parse(sourceXml);
        var xml = new SanimXmlValues(document);
        var config = DebtPaperMappingConfig.forPaperType(details.getPaperType());
        String generalName = required(details.getGeneralDirectorateName(), "general directorate");
        GeneralDirectorateOfTaxAffairs general = generalDirectorates.findAll().stream()
                .filter(item -> generalName.equals(item.getGeneralDirectorateName()))
                .findFirst().orElseGet(() -> generalDirectorates.save(GeneralDirectorateOfTaxAffairs.builder()
                        .generalDirectorateName(generalName).postalCode("").address("").phone("").build()));

        String officeName = required(details.getTaxOfficeName(), "tax office");
        String[] officeTags = details.getPaperType() == PaperType.EF03
                ? new String[]{"J", "K", "L"} : new String[]{"H", "I", "J"};
        String phone = value(xml.text(officeTags[0]));
        String postalCode = value(xml.text(officeTags[1]));
        String address = value(xml.text(officeTags[2]));
        TaxAffairsOffice office = taxOffices.findByGeneralDirectorateOfTaxAffairsId(general.getId()).stream()
                .filter(item -> officeName.equals(item.getOfficeName())).findFirst().orElse(null);
        if (office == null) {
            taxOffices.save(TaxAffairsOffice.builder().officeName(officeName)
                    .phone(phone).postalCode(postalCode).address(address)
                    .generalDirectorateOfTaxAffairs(general).build());
        } else if (changed(office.getPhone(), phone)
                || changed(office.getPostalCode(), postalCode)
                || changed(office.getAddress(), address)) {
            taxOfficeHistory.save(TaxAffairsOfficeHistory.from(office, "SAMPLE_IMPORT_UPDATE"));
            if (!phone.isBlank()) office.setPhone(phone);
            if (!postalCode.isBlank()) office.setPostalCode(postalCode);
            if (!address.isBlank()) office.setAddress(address);
            taxOffices.save(office);
        }

        String activityTitle = value(config.text(xml, ACTIVITY_TITLE));
        if (!activityTitle.isBlank() && activityTypes.findByTitle(activityTitle).isEmpty()) {
            activityTypes.save(ActivityType.builder()
                    .intaCode(details.getPaperType().name() + "-" + shortHash(activityTitle))
                    .title(activityTitle).build());
        }
    }

    private String message(RuntimeException exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }

    private String required(String value, String field) {
        String result = value(value);
        if (result.isBlank()) throw new IllegalArgumentException("Missing " + field);
        return result;
    }

    private String value(String value) {
        return value == null || value.isBlank() || "--".equals(value.trim()) ? "" : value.trim();
    }

    private boolean changed(String current, String incoming) {
        return !incoming.isBlank() && !Objects.equals(current, incoming);
    }

    private String shortHash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (Exception exception) { throw new IllegalStateException(exception); }
    }

    public record ImportReport(int requested, int imported, int skipped, List<String> failures) {
        public int failed() { return failures.size(); }
    }
}
