package com.mparsict.vosolmaliat.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.integration.CompanyRegistryRelatedPersonData;
import com.mparsict.vosolmaliat.application.dto.integration.CompanyRegistryNaturalPersonData;
import com.mparsict.vosolmaliat.application.dto.response.ExecutionPaperRelatedPersonsResponse;
import com.mparsict.vosolmaliat.application.dto.response.RelatedPersonResponse;
import com.mparsict.vosolmaliat.application.service.security.AuditLogService;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.domain.enums.RelatedPersonType;
import com.mparsict.vosolmaliat.domain.model.Taxpayer;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import com.mparsict.vosolmaliat.domain.model.execution.FinalTaxDebtPaper;
import com.mparsict.vosolmaliat.domain.model.execution.RelatedPerson;
import com.mparsict.vosolmaliat.domain.model.execution.RelatedPersonPost;
import com.mparsict.vosolmaliat.domain.model.person.LegalPerson;
import com.mparsict.vosolmaliat.domain.model.person.NaturalPerson;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
import com.mparsict.vosolmaliat.domain.repository.FinalTaxDebtPaperRepository;
import com.mparsict.vosolmaliat.domain.repository.LegalPersonRepository;
import com.mparsict.vosolmaliat.domain.repository.NaturalPersonRepository;
import com.mparsict.vosolmaliat.domain.repository.RelatedPersonRepository;
import com.mparsict.vosolmaliat.domain.repository.RelatedPersonPostRepository;
import com.mparsict.vosolmaliat.domain.repository.TaxpayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class RelatedPersonService {

    private static final String SOURCE_COMPANY_REGISTRY = "FAKE_COMPANY_REGISTRY";
    private static final String SOURCE_OFFICIAL_GAZETTE = "FAKE_OFFICIAL_GAZETTE";

    private final ExecutionPaperRepository executionPaperRepository;
    private final TaxpayerRepository taxpayerRepository;
    private final FinalTaxDebtPaperRepository finalTaxDebtPaperRepository;
    private final NaturalPersonRepository naturalPersonRepository;
    private final LegalPersonRepository legalPersonRepository;
    private final RelatedPersonPostRepository relatedPersonPostRepository;
    private final RelatedPersonRepository relatedPersonRepository;
    private final FakeCompanyRegistryService fakeCompanyRegistryService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ExecutionPaperRelatedPersonsResponse getForExecutionPaper(Long executionPaperId) {
        ExecutionContext context = executionContext(executionPaperId);
        return response(context);
    }

    @Transactional
    public ExecutionPaperRelatedPersonsResponse refreshForExecutionPaper(Long executionPaperId) {
        ExecutionContext context = executionContext(executionPaperId);
        List<RelatedPerson> existingRelationships =
                relatedPersonRepository.findByOwnerLegalPersonIdOrderByStartDateAscIdAsc(
                        context.owner().getId()
                );
        expireRandomActiveRelationships(existingRelationships, context.debtYear());
        List<CompanyRegistryRelatedPersonData> externalData =
                fakeCompanyRegistryService.fetchNewRelatedPersons(
                        context.owner().getNationalId(),
                        existingRelationships.size() + 1L,
                        debtYearStart(context.debtYear())
                );
        synchronize(context, externalData, SOURCE_COMPANY_REGISTRY);

        ExecutionPaperRelatedPersonsResponse refreshed = response(context);
        recordSynchronizationAudit(
                executionPaperId,
                "RELATED_PERSONS_REFRESHED_FROM_COMPANY_REGISTRY",
                refreshed
        );
        return refreshed;
    }

    private void expireRandomActiveRelationships(
            List<RelatedPerson> existingRelationships,
            String debtYear) {
        List<RelatedPerson> activeRelationships = new ArrayList<>(
                existingRelationships.stream()
                        .filter(item -> item.getEndDate() == null)
                        .toList()
        );
        if (activeRelationships.isEmpty()) {
            return;
        }

        Collections.shuffle(activeRelationships);
        int maximumToExpire = Math.min(2, activeRelationships.size());
        int countToExpire = maximumToExpire == 1
                ? 1
                : ThreadLocalRandom.current().nextInt(1, maximumToExpire + 1);
        activeRelationships.stream()
                .limit(countToExpire)
                .forEach(relationship -> {
                    relationship.setEndDate(randomExpiryDate(relationship, debtYear));
                    relatedPersonRepository.save(relationship);
                });
    }

    private String debtYearStart(String debtYear) {
        return hasText(debtYear) ? debtYear + "-01-01" : "1400-01-01";
    }

    private LocalDateTime randomExpiryDate(RelatedPerson relationship, String debtYear) {
        String year = hasText(debtYear) ? debtYear : "1400";
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int month = random.nextInt(1, 13);
        int maximumDay = month <= 6 ? 31 : month <= 11 ? 30 : 29;
        int day = random.nextInt(1, maximumDay + 1);
        String candidate = "%s-%02d-%02d".formatted(year, month, day);
        LocalDateTime candidateDate = com.mparsict.vosolmaliat.domain.value.PersianDates.parse(candidate);
        LocalDateTime startDate = relationship.getStartDate();
        return startDate != null && startDate.isAfter(candidateDate) ? startDate : candidateDate;
    }

    @Transactional
    public ExecutionPaperRelatedPersonsResponse storeOfficialGazetteRelatedPersons(
            Long executionPaperId,
            List<CompanyRegistryRelatedPersonData> relatedPersons) {
        ExecutionContext context = executionContext(executionPaperId);
        synchronize(context, relatedPersons, SOURCE_OFFICIAL_GAZETTE);

        ExecutionPaperRelatedPersonsResponse stored = response(context);
        recordSynchronizationAudit(
                executionPaperId,
                "RELATED_PERSONS_STORED_FROM_OFFICIAL_GAZETTE",
                stored
        );
        return stored;
    }

    private void synchronize(
            ExecutionContext context,
            List<CompanyRegistryRelatedPersonData> externalData,
            String sourceSystem) {
        for (CompanyRegistryRelatedPersonData item : externalData) {
            validateExternalItem(item);
            RelatedPersonPost post = resolvePost(item);
            LocalDateTime startDate = parseJalaliDate(item.startDate());
            NaturalPerson naturalPerson = item.personType() == RelatedPersonType.NATURAL
                    ? resolveNaturalPerson(item) : null;
            LegalPerson legalPerson = item.personType() == RelatedPersonType.LEGAL
                    ? resolveLegalPerson(item) : null;
            RelatedPerson relationship = (naturalPerson != null
                    ? relatedPersonRepository.findByOwnerLegalPersonIdAndPostIdAndNaturalPersonIdAndStartDate(
                            context.owner().getId(), post.getId(), naturalPerson.getId(), startDate)
                    : relatedPersonRepository.findByOwnerLegalPersonIdAndPostIdAndLegalPersonIdAndStartDate(
                            context.owner().getId(), post.getId(), legalPerson.getId(), startDate))
                    .orElseGet(RelatedPerson::new);

            relationship.setOwnerLegalPerson(context.owner());
            relationship.setPost(post);
            relationship.setStartDate(startDate);
            relationship.setEndDate(parseJalaliDate(item.endDate()));
            relationship.setPositionExpiryDate(parseJalaliDate(item.endDate()));

            if (naturalPerson != null) {
                relationship.setNaturalPerson(naturalPerson);
                relationship.setLegalPerson(null);
                relationship.setLegalRepresentativeNaturalPerson(null);
            } else {
                relationship.setLegalPerson(legalPerson);
                relationship.setNaturalPerson(null);
                relationship.setLegalRepresentativeNaturalPerson(
                        resolveNaturalPerson(item.legalRepresentative())
                );
            }
            relatedPersonRepository.save(relationship);
        }
    }

    private void recordSynchronizationAudit(
            Long executionPaperId,
            String action,
            ExecutionPaperRelatedPersonsResponse response) {
        auditLogService.record(
                null,
                action,
                "EXECUTION_PAPER",
                executionPaperId.toString(),
                null,
                toAuditPayload(response)
        );
    }

    private ExecutionPaperRelatedPersonsResponse response(ExecutionContext context) {
        List<RelatedPerson> allRelationships =
                relatedPersonRepository.findByOwnerLegalPersonIdOrderByStartDateAscIdAsc(context.owner().getId());
        List<RelatedPersonResponse> applicableRelationships = allRelationships.stream()
                .filter(item -> overlapsDebtYear(item, context.debtYear()))
                .map(this::toResponse)
                .toList();
        String lastSyncedAt = allRelationships.stream()
                .map(RelatedPerson::getUpdatedAt)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .map(LocalDateTime::toString)
                .orElse(null);

        return new ExecutionPaperRelatedPersonsResponse(
                context.executionPaper().getId(),
                context.owner().getId(),
                context.owner().getNationalId(),
                context.debtYear(),
                lastSyncedAt,
                applicableRelationships
        );
    }

    private ExecutionContext executionContext(Long executionPaperId) {
        ExecutionPaper executionPaper = executionPaperRepository.findById(executionPaperId)
                .orElseThrow(() -> new BusinessException("EXECUTION_PAPER_NOT_FOUND", "برگ اجرا یافت نشد"));
        Long taxpayerId = executionPaper.getFinalTaxDebtPaper() == null
                || executionPaper.getFinalTaxDebtPaper().getTaxpayer() == null
                ? null : executionPaper.getFinalTaxDebtPaper().getTaxpayer().getId();
        Taxpayer taxpayer = taxpayerRepository.findById(taxpayerId)
                .orElseThrow(() -> new BusinessException("TAXPAYER_NOT_FOUND", "مودی برگ اجرا یافت نشد"));
        if (taxpayer.getLegalPersonId() == null) {
            throw new BusinessException(
                    "RELATED_PERSONS_ONLY_FOR_LEGAL_TAXPAYER",
                    "اشخاص مرتبط فقط برای مودی حقوقی قابل دریافت هستند"
            );
        }
        LegalPerson owner = legalPersonRepository.findById(taxpayer.getLegalPersonId())
                .orElseThrow(() -> new BusinessException("LEGAL_PERSON_NOT_FOUND", "شخص حقوقی مودی یافت نشد"));
        FinalTaxDebtPaper finalTaxDebtPaper =
                finalTaxDebtPaperRepository.findById(executionPaper.getFinalTaxDebtPaperId())
                        .orElseThrow(() -> new BusinessException(
                                "FINALIZED_DEBT_PAPER_NOT_FOUND",
                                "برگ بدهی قطعی مرتبط یافت نشد"
                        ));
        return new ExecutionContext(executionPaper, taxpayer, owner,
                normalizeYear(String.valueOf(finalTaxDebtPaper.getTaxYear())));
    }

    private NaturalPerson resolveNaturalPerson(CompanyRegistryRelatedPersonData item) {
        return resolveNaturalPerson(new CompanyRegistryNaturalPersonData(
                item.nationalCode(),
                item.fida(),
                item.firstName(),
                item.lastName(),
                item.fatherName(),
                item.birthDate()
        ));
    }

    private NaturalPerson resolveNaturalPerson(CompanyRegistryNaturalPersonData item) {
        NaturalPerson person = !hasText(item.nationalCode())
                ? naturalPersonRepository.findByFida(item.fida()).orElseGet(NaturalPerson::new)
                : naturalPersonRepository.findByNationalCode(item.nationalCode()).orElseGet(NaturalPerson::new);
        person.setNationalCode(blankToNull(item.nationalCode()));
        person.setFida(blankToNull(item.fida()));
        person.updateProfile(defaultString(item.firstName()),defaultString(item.lastName()),
                defaultString(item.fatherName()),defaultString(normalizeJalaliDate(item.birthDate())));
        return naturalPersonRepository.save(person);
    }

    private LegalPerson resolveLegalPerson(CompanyRegistryRelatedPersonData item) {
        LegalPerson person = item.nationalId() == null
                ? legalPersonRepository.findByRegistrationNumber(item.registrationNumber()).orElseGet(LegalPerson::new)
                : legalPersonRepository.findByNationalId(item.nationalId()).orElseGet(LegalPerson::new);
        person.setNationalId(blankToNull(item.nationalId()));
        person.updateProfile(defaultString(item.companyName()),blankToNull(item.registrationNumber()),
                defaultString(normalizeJalaliDate(item.registrationDate())));
        return legalPersonRepository.save(person);
    }

    private void validateExternalItem(CompanyRegistryRelatedPersonData item) {
        if (item.personType() == null || item.postCode() == null) {
            throw new BusinessException("COMPANY_REGISTRY_DATA_INVALID", "نوع شخص یا سمت شخص مرتبط نامعتبر است");
        }
        if (!item.postCode().supports(item.personType())) {
            throw new BusinessException(
                    "RELATED_PERSON_POST_NOT_ALLOWED",
                    "سمت " + item.postCode().label() + " برای این نوع شخص مرتبط مجاز نیست"
            );
        }
        boolean naturalIdentifierPresent = hasText(item.nationalCode()) || hasText(item.fida());
        boolean legalIdentifierPresent = hasText(item.nationalId()) || hasText(item.registrationNumber());
        if (item.personType() == RelatedPersonType.NATURAL && !naturalIdentifierPresent) {
            throw new BusinessException("RELATED_NATURAL_PERSON_IDENTIFIER_REQUIRED", "شناسه شخص حقیقی مرتبط الزامی است");
        }
        if (item.personType() == RelatedPersonType.LEGAL && !legalIdentifierPresent) {
            throw new BusinessException("RELATED_LEGAL_PERSON_IDENTIFIER_REQUIRED", "شناسه شخص حقوقی مرتبط الزامی است");
        }
        if (item.personType() == RelatedPersonType.NATURAL && item.legalRepresentative() != null) {
            throw new BusinessException(
                    "RELATED_NATURAL_PERSON_REPRESENTATIVE_NOT_ALLOWED",
                    "نماینده حقیقی فقط برای شخص حقوقی مرتبط قابل ثبت است"
            );
        }
        if (item.personType() == RelatedPersonType.LEGAL) {
            CompanyRegistryNaturalPersonData representative = item.legalRepresentative();
            if (representative == null
                    || (!hasText(representative.nationalCode()) && !hasText(representative.fida()))) {
                throw new BusinessException(
                        "RELATED_LEGAL_PERSON_REPRESENTATIVE_REQUIRED",
                        "نماینده حقیقی شخص حقوقی مرتبط الزامی است"
                );
            }
        }
    }

    private RelatedPersonPost resolvePost(CompanyRegistryRelatedPersonData item) {
        return relatedPersonPostRepository.findByCode(item.postCode())
                .orElseThrow(() -> new BusinessException(
                        "RELATED_PERSON_POST_NOT_CONFIGURED",
                        "سمت " + item.postCode().label() + " در جدول سمت‌ها تعریف نشده است"
                ));
    }

    private boolean overlapsDebtYear(RelatedPerson relationship, String debtYear) {
        if (!hasText(debtYear)) {
            return true;
        }
        LocalDateTime yearStart = com.mparsict.vosolmaliat.domain.value.PersianDates.parse(debtYear + "-01-01");
        LocalDateTime nextYearStart = com.mparsict.vosolmaliat.domain.value.PersianDates.parse(
                (Integer.parseInt(debtYear) + 1) + "-01-01");
        LocalDateTime startDate = relationship.getStartDate();
        LocalDateTime endDate = relationship.getEndDate();
        return (startDate == null || startDate.isBefore(nextYearStart))
                && (endDate == null || !endDate.isBefore(yearStart));
    }

    private RelatedPersonResponse toResponse(RelatedPerson relationship) {
        NaturalPerson naturalPerson = relationship.getNaturalPerson();
        LegalPerson legalPerson = relationship.getLegalPerson();
        NaturalPerson legalRepresentative = relationship.getLegalRepresentativeNaturalPerson();
        RelatedPersonPost post = relationship.getPost();
        String personType = naturalPerson != null ? RelatedPersonType.NATURAL.name() : RelatedPersonType.LEGAL.name();
        return new RelatedPersonResponse(
                relationship.getId(),
                personType,
                post == null ? null : post.getId(),
                post == null ? null : post.getCodeInCompanyRegistrationSystem(),
                post == null ? null : post.getTitle(),
                formatJalaliDate(relationship.getStartDate()),
                formatJalaliDate(relationship.getEndDate()),
                false,
                naturalPerson == null ? null : naturalPerson.getId(),
                naturalPerson == null ? null : naturalPerson.getNationalCode(),
                naturalPerson == null ? null : naturalPerson.getFida(),
                naturalPerson == null ? null : naturalPerson.getFirstName(),
                naturalPerson == null ? null : naturalPerson.getLastName(),
                naturalPerson == null ? null : naturalPerson.getFatherName(),
                naturalPerson == null ? null : naturalPerson.getBirthDate(),
                legalPerson == null ? null : legalPerson.getId(),
                legalPerson == null ? null : legalPerson.getNationalId(),
                legalPerson == null ? null : legalPerson.getRegistrationNumber(),
                legalPerson == null ? null : legalPerson.getRegistrationDate(),
                legalPerson == null ? null : legalPerson.getName(),
                legalRepresentative == null ? null : legalRepresentative.getId(),
                legalRepresentative == null ? null : legalRepresentative.getNationalCode(),
                legalRepresentative == null ? null : legalRepresentative.getFida(),
                legalRepresentative == null ? null : legalRepresentative.getFirstName(),
                legalRepresentative == null ? null : legalRepresentative.getLastName(),
                legalRepresentative == null ? null : legalRepresentative.getFatherName(),
                legalRepresentative == null ? null : legalRepresentative.getBirthDate()
        );
    }

    private String toAuditPayload(ExecutionPaperRelatedPersonsResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(
                    "RELATED_PERSON_AUDIT_PAYLOAD_ERROR",
                    "خطا در ثبت سابقه اشخاص مرتبط: " + ex.getMessage(),
                    ex
            );
        }
    }

    private String normalizeYear(String value) {
        if (!hasText(value)) {
            return null;
        }
        return toEnglishDigits(value.trim()).replaceAll("[^0-9]", "");
    }

    private String normalizeJalaliDate(String value) {
        if (!hasText(value)) {
            return null;
        }
        return toEnglishDigits(value.trim()).replace('/', '-');
    }

    private LocalDateTime parseJalaliDate(String value) {
        String normalized = normalizeJalaliDate(value);
        return hasText(normalized) ? com.mparsict.vosolmaliat.domain.value.PersianDates.parse(normalized) : null;
    }

    private String formatJalaliDate(LocalDateTime value) {
        return value == null ? null : com.mparsict.vosolmaliat.domain.value.PersianDates.format(value);
    }

    private String toEnglishDigits(String value) {
        return value
                .replace('۰', '0')
                .replace('۱', '1')
                .replace('۲', '2')
                .replace('۳', '3')
                .replace('۴', '4')
                .replace('۵', '5')
                .replace('۶', '6')
                .replace('۷', '7')
                .replace('۸', '8')
                .replace('۹', '9');
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String defaultString(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record ExecutionContext(
            ExecutionPaper executionPaper,
            Taxpayer taxpayer,
            LegalPerson owner,
            String debtYear
    ) {
    }
}
