package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.application.dto.request.DebtPaperPushRequest;
import com.mparsict.vosolmaliat.domain.model.masterdata.ActivityType;
import com.mparsict.vosolmaliat.domain.model.masterdata.GeneralDirectorateOfTaxAffairs;
import com.mparsict.vosolmaliat.domain.model.masterdata.TaxAffairsOffice;
import com.mparsict.vosolmaliat.domain.repository.ActivityTypeRepository;
import com.mparsict.vosolmaliat.domain.repository.DebtPaperPushRecordRepository;
import com.mparsict.vosolmaliat.domain.repository.GeneralDirectorateOfTaxAffairsRepository;
import com.mparsict.vosolmaliat.domain.repository.TaxAffairsOfficeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class Ef03SampleImportService {
    private final Ef03SampleService samples;
    private final DebtPaperPushMapper mapper;
    private final DebtPaperPushPersistenceService persistence;
    private final ExecutionPaperService executionPapers;
    private final DebtPaperPushRecordRepository pushRecords;
    private final DebtPaperXmlReader xmlReader;
    private final GeneralDirectorateOfTaxAffairsRepository generalDirectorates;
    private final TaxAffairsOfficeRepository taxOffices;
    private final ActivityTypeRepository activityTypes;

    public ImportReport importAll() {
        int imported = 0;
        int skipped = 0;
        List<String> failures = new ArrayList<>();
        OffsetDateTime receivedAt = OffsetDateTime.now(ZoneOffset.UTC);

        for (Ef03SampleService.Sample sample : samples.samples()) {
            Long recordId = null;
            try {
                DebtPaperPushRequest input = request(sample, receivedAt);
                var existing = pushRecords.findByReferenceId(sample.referenceID());
                if (existing.isPresent()
                        && existing.get().getProcessingStatus()
                        == com.mparsict.vosolmaliat.domain.enums.DebtPaperProcessingStatus.SUCCESS) {
                    skipped++;
                    continue;
                }
                recordId = existing.isPresent()
                        ? persistence.resumeSampleImport(existing.get().getId())
                        : persistence.receive(input, "ef03-sample-import-" + sample.key());
                var request = mapper.map(input);
                ensureReferenceData(request.sourceDocument(), sample.xml());
                persistence.prepareWorkflow(recordId, request);
                var executionPaper = executionPapers.createFromRequest(request);
                persistence.markSuccessful(recordId, executionPaper.getId());
                imported++;
            } catch (RuntimeException exception) {
                if (recordId != null) persistence.markNeedsReview(recordId, exception.getMessage());
                failures.add(sample.referenceID() + ": " + exception.getMessage());
            }
        }
        return new ImportReport(samples.samples().size(), imported, skipped, failures);
    }

    private DebtPaperPushRequest request(Ef03SampleService.Sample sample, OffsetDateTime receivedAt) {
        return new DebtPaperPushRequest(
                sample.nationalID(), sample.xml(), sample.hash(), receivedAt,
                sample.referenceID(), sample.followCode(), sample.secondParameter(),
                sample.year(), sample.period(), sample.taxOfficeID(), sample.taxAmount(),
                sample.taxSource(), sample.announcementType());
    }

    private void ensureReferenceData(
            com.mparsict.vosolmaliat.application.dto.request.SanimDocumentDetails details,
            String sourceXml) {
        var xml = new SanimXmlValues(xmlReader.parse(sourceXml));
        String generalName = required(details.getGeneralDirectorateName(), "general directorate");
        GeneralDirectorateOfTaxAffairs general = generalDirectorates.findAll().stream()
                .filter(item -> generalName.equals(item.getGeneralDirectorateName()))
                .findFirst()
                .orElseGet(() -> generalDirectorates.save(GeneralDirectorateOfTaxAffairs.builder()
                        .generalDirectorateName(generalName)
                        .postalCode("")
                        .address("")
                        .phone("")
                        .build()));

        String officeName = required(details.getTaxOfficeName(), "tax office");
        boolean officeExists = taxOffices.findAll().stream().anyMatch(item ->
                officeName.equals(item.getOfficeName())
                        && general.getId().equals(item.getGeneralDirectorateOfTaxAffairsId()));
        if (!officeExists) {
            taxOffices.save(TaxAffairsOffice.builder()
                    .officeName(officeName)
                    .postalCode(value(xml.text("J")))
                    .phone(value(xml.text("K")))
                    .address(value(xml.text("L")))
                    .generalDirectorateOfTaxAffairs(general)
                    .build());
        }

        String activityTitle = value(details.getActivityTitle());
        if (!activityTitle.isBlank() && activityTypes.findByTitle(activityTitle).isEmpty()) {
            activityTypes.save(ActivityType.builder()
                    .intaCode("EF03-" + shortHash(activityTitle))
                    .title(activityTitle)
                    .build());
        }
    }

    private String shortHash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String required(String value, String field) {
        String normalized = blankToNull(value);
        if (normalized == null) throw new IllegalArgumentException("Missing " + field);
        return normalized;
    }

    private String value(String value) {
        String normalized = blankToNull(value);
        return normalized == null || "--".equals(normalized) ? "" : normalized;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record ImportReport(int total, int imported, int skipped, List<String> failures) {
        public int failed() {
            return failures.size();
        }
    }
}
