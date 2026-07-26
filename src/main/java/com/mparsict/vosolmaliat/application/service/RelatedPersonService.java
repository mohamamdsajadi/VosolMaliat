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
import com.mparsict.vosolmaliat.domain.model.execution.FinalizedDebtPaper;
import com.mparsict.vosolmaliat.domain.model.execution.RelatedPerson;
import com.mparsict.vosolmaliat.domain.model.execution.RelatedPersonPost;
import com.mparsict.vosolmaliat.domain.model.person.LegalPerson;
import com.mparsict.vosolmaliat.domain.model.person.NaturalPerson;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
import com.mparsict.vosolmaliat.domain.repository.FinalizedDebtPaperRepository;
import com.mparsict.vosolmaliat.domain.repository.LegalPersonRepository;
import com.mparsict.vosolmaliat.domain.repository.NaturalPersonRepository;
import com.mparsict.vosolmaliat.domain.repository.RelatedPersonRepository;
import com.mparsict.vosolmaliat.domain.repository.RelatedPersonPostRepository;
import com.mparsict.vosolmaliat.domain.repository.TaxpayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RelatedPersonService {

    private static final String SOURCE_SYSTEM = "FAKE_COMPANY_REGISTRY";

    private final ExecutionPaperRepository executionPaperRepository;
    private final TaxpayerRepository taxpayerRepository;
    private final FinalizedDebtPaperRepository finalizedDebtPaperRepository;
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
        List<CompanyRegistryRelatedPersonData> externalData =
                fakeCompanyRegistryService.fetchRelatedPersons(context.owner().getNationalId());
        String syncedAt = LocalDateTime.now().toString();

        for (CompanyRegistryRelatedPersonData item : externalData) {
            validateExternalItem(item);
            RelatedPerson relationship = relatedPersonRepository
                    .findByOwnerLegalPersonIdAndSourceSystemAndSourceRecordId(
                            context.owner().getId(),
                            SOURCE_SYSTEM,
                            item.sourceRecordId()
                    )
                    .orElseGet(RelatedPerson::new);

            relationship.setOwnerLegalPerson(context.owner());
            relationship.setPost(resolvePost(item));
            relationship.setStartDate(normalizeJalaliDate(item.startDate()));
            relationship.setEndDate(normalizeJalaliDate(item.endDate()));
            relationship.setHasSignatoryAuthority(item.hasSignatoryAuthority() ? 1 : 0);
            relationship.setSourceSystem(SOURCE_SYSTEM);
            relationship.setSourceRecordId(item.sourceRecordId());
            relationship.setLastSyncedAt(syncedAt);

            if (item.personType() == RelatedPersonType.NATURAL) {
                relationship.setNaturalPerson(resolveNaturalPerson(item));
                relationship.setLegalPerson(null);
                relationship.setLegalRepresentativeNaturalPerson(null);
            } else {
                relationship.setLegalPerson(resolveLegalPerson(item));
                relationship.setNaturalPerson(null);
                relationship.setLegalRepresentativeNaturalPerson(
                        resolveNaturalPerson(item.legalRepresentative())
                );
            }
            relatedPersonRepository.save(relationship);
        }

        ExecutionPaperRelatedPersonsResponse refreshed = response(context);
        auditLogService.record(
                null,
                "RELATED_PERSONS_REFRESHED_FROM_COMPANY_REGISTRY",
                "EXECUTION_PAPER",
                executionPaperId.toString(),
                null,
                toAuditPayload(refreshed)
        );
        return refreshed;
    }

    private ExecutionPaperRelatedPersonsResponse response(ExecutionContext context) {
        List<RelatedPerson> allRelationships =
                relatedPersonRepository.findByOwnerLegalPersonIdOrderByStartDateAscIdAsc(context.owner().getId());
        List<RelatedPersonResponse> applicableRelationships = allRelationships.stream()
                .filter(item -> overlapsDebtYear(item, context.debtYear()))
                .map(this::toResponse)
                .toList();
        String lastSyncedAt = allRelationships.stream()
                .map(RelatedPerson::getLastSyncedAt)
                .filter(value -> value != null && !value.isBlank())
                .max(String::compareTo)
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
        Taxpayer taxpayer = taxpayerRepository.findById(executionPaper.getTaxpayerId())
                .orElseThrow(() -> new BusinessException("TAXPAYER_NOT_FOUND", "مودی برگ اجرا یافت نشد"));
        if (taxpayer.getLegalPersonId() == null) {
            throw new BusinessException(
                    "RELATED_PERSONS_ONLY_FOR_LEGAL_TAXPAYER",
                    "اشخاص مرتبط فقط برای مودی حقوقی قابل دریافت هستند"
            );
        }
        LegalPerson owner = legalPersonRepository.findById(taxpayer.getLegalPersonId())
                .orElseThrow(() -> new BusinessException("LEGAL_PERSON_NOT_FOUND", "شخص حقوقی مودی یافت نشد"));
        FinalizedDebtPaper finalizedDebtPaper =
                finalizedDebtPaperRepository.findById(executionPaper.getFinalizedDebtPaperId())
                        .orElseThrow(() -> new BusinessException(
                                "FINALIZED_DEBT_PAPER_NOT_FOUND",
                                "برگ بدهی قطعی مرتبط یافت نشد"
                        ));
        return new ExecutionContext(executionPaper, owner, normalizeYear(finalizedDebtPaper.getPerformanceYear()));
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
        person.setFirstName(defaultString(item.firstName()));
        person.setLastName(defaultString(item.lastName()));
        person.setFatherName(defaultString(item.fatherName()));
        person.setBirthDate(defaultString(normalizeJalaliDate(item.birthDate())));
        return naturalPersonRepository.save(person);
    }

    private LegalPerson resolveLegalPerson(CompanyRegistryRelatedPersonData item) {
        LegalPerson person = item.nationalId() == null
                ? legalPersonRepository.findByRegistrationNumber(item.registrationNumber()).orElseGet(LegalPerson::new)
                : legalPersonRepository.findByNationalId(item.nationalId()).orElseGet(LegalPerson::new);
        person.setNationalId(blankToNull(item.nationalId()));
        person.setRegistrationNumber(blankToNull(item.registrationNumber()));
        person.setRegistrationDate(defaultString(normalizeJalaliDate(item.registrationDate())));
        person.setName(defaultString(item.companyName()));
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
        String yearStart = debtYear + "-01-01";
        String nextYearStart = (Integer.parseInt(debtYear) + 1) + "-01-01";
        String startDate = normalizeJalaliDate(relationship.getStartDate());
        String endDate = normalizeJalaliDate(relationship.getEndDate());
        return (!hasText(startDate) || startDate.compareTo(nextYearStart) < 0)
                && (!hasText(endDate) || endDate.compareTo(yearStart) >= 0);
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
                post == null || post.getCode() == null ? null : post.getCode().name(),
                post == null || post.getCode() == null ? null : post.getCode().label(),
                relationship.getStartDate(),
                relationship.getEndDate(),
                Integer.valueOf(1).equals(relationship.getHasSignatoryAuthority()),
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
            LegalPerson owner,
            String debtYear
    ) {
    }
}
