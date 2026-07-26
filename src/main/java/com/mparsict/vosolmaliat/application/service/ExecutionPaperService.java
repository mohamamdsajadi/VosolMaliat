package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.application.dto.request.ExecutionPaperSearchCriteria;
import com.mparsict.vosolmaliat.application.dto.response.ExecutionPaperDetailResponse;
import com.mparsict.vosolmaliat.application.dto.response.ExecutionPaperDto;
import com.mparsict.vosolmaliat.application.dto.response.ExecutionPaperPageResponse;
import com.mparsict.vosolmaliat.application.dto.response.ExecutionPaperTraceItemResponse;
import com.mparsict.vosolmaliat.application.dto.response.ExecutionPaperTraceResponse;
import com.mparsict.vosolmaliat.application.service.security.AuditLogService;
import com.mparsict.vosolmaliat.common.util.UniqueCodeGenerator;
import com.mparsict.vosolmaliat.domain.enums.ExecutionPaperState;
import com.mparsict.vosolmaliat.domain.enums.TaxpayerType;
import com.mparsict.vosolmaliat.domain.enums.TaxSourceType;
import com.mparsict.vosolmaliat.domain.model.ContactInfo;
import com.mparsict.vosolmaliat.domain.model.Taxpayer;
import com.mparsict.vosolmaliat.domain.model.execution.*;
import com.mparsict.vosolmaliat.domain.model.masterdata.TaxUnit;
import com.mparsict.vosolmaliat.domain.model.security.AuditLog;
import com.mparsict.vosolmaliat.domain.model.person.LegalPerson;
import com.mparsict.vosolmaliat.domain.model.person.NaturalPerson;
import com.mparsict.vosolmaliat.domain.repository.*;
import com.mparsict.vosolmaliat.domain.repository.security.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionPaperService {

    private final ExecutionPaperRepository executionPaperRepository;
    private final FinalizedDebtPaperRepository finalizedDebtPaperRepository;
    private final TaxpayerRepository taxpayerRepository;
    private final ContactInfoRepository contactInfoRepository;
    private final NaturalPersonRepository naturalPersonRepository;
    private final LegalPersonRepository legalPersonRepository;
    private final DebtInformationRepository debtInformationRepository;
    private final ExecutionPaperStateHistoryRepository stateHistoryRepository;
    private final ExecutionPaperNotificationRepository executionPaperNotificationRepository;
    private final GeneralDirectorateOfTaxAffairsRepository generalDirectorateRepository;
    private final TaxAffairsOfficeRepository taxAffairsOfficeRepository;
    private final TaxUnitRepository taxUnitRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogService;
    private final UniqueCodeGenerator uniqueCodeGenerator;
    private final ExecutionPaperIssuanceRequestRepository issuanceRequestRepository;

    @Transactional
    public ExecutionPaper createFromRequest(DebtPaperIssuanceRequest request) {
        // 1. Find or create FinalizedDebtPaper
        FinalizedDebtPaper fdp = finalizedDebtPaperRepository
                .findByDebtPaperUniqueCode(request.debtPaperUniqueCode())
                .orElseGet(() -> finalizedDebtPaperRepository.save(
                        FinalizedDebtPaper.builder()
                                .debtPaperUniqueCode(request.debtPaperUniqueCode())
                                .debtPaperIssueDate(request.debtPaperIssueDate())
                                .notificationId(request.notificationId())
                                .finalizationReason(request.finalizationReason())
                                .taxSource(request.taxSource())
                                .performanceYear(request.performanceYear())
                                .performancePeriod(request.performancePeriod())
                                .activityTypeCode(request.activityTypeCode())
                                .activityTypeTitle(request.activityTypeTitle())
                                .mainTaxBureau(request.mainTaxBureauId() == null ? null : com.mparsict.vosolmaliat.domain.model.masterdata.GeneralDirectorateOfTaxAffairs.builder().id(request.mainTaxBureauId()).build())
                                .taxBureau(request.taxBureauId() == null ? null : com.mparsict.vosolmaliat.domain.model.masterdata.TaxAffairsOffice.builder().id(request.taxBureauId()).build())
                                .taxUnit(request.taxUnitId() == null ? null : TaxUnit.builder().id(request.taxUnitId()).build())
                                .totalFinalizedAmount(computeTotalFinalizedAmount(request.debtInfo()))
                                .finalizedTaxAndNonDeductibleAmount(computeFinalizedTaxAndNonDeductibleAmount(request.debtInfo()))
                                .build()
                ));

        // 2. Find or create the person and taxpayer
        Long naturalPersonId = null;
        Long legalPersonId = null;
        DebtPaperIssuanceRequest.TaxpayerInfo tpInfo = request.taxpayer();

        if ("NATURAL".equalsIgnoreCase(tpInfo.type())) {
            NaturalPerson np = resolveNaturalPerson(tpInfo);
            naturalPersonId = np.getId();
        } else {
            LegalPerson lp = resolveLegalPerson(tpInfo);
            legalPersonId = lp.getId();
        }

        final Long finalNaturalPersonId = naturalPersonId;
        final Long finalLegalPersonId = legalPersonId;

        Taxpayer taxpayer = taxpayerRepository
                .findByDebtPaperUniqueCode(request.debtPaperUniqueCode())
                .orElseGet(() -> taxpayerRepository.save(
                        Taxpayer.builder()
                                .debtPaperUniqueCode(request.debtPaperUniqueCode())
                                .debtPaperIssueDate(request.debtPaperIssueDate())
                                .naturalPerson(finalNaturalPersonId == null ? null : NaturalPerson.builder().id(finalNaturalPersonId).build())
                                .legalPerson(finalLegalPersonId == null ? null : LegalPerson.builder().id(finalLegalPersonId).build())
                                .build()
                ));
        taxpayer = taxpayerRepository.save(taxpayer);
        upsertContactInfo(tpInfo, finalNaturalPersonId, finalLegalPersonId);

        // 3. Create ExecutionPaper with UNASSIGNED state
        String epCode = uniqueCodeGenerator.generateExecutionPaperCode();
        ExecutionPaper ep = ExecutionPaper.builder()
                .executionPaperUniqueCode(epCode)
                .finalizedDebtPaper(fdp)
                .taxpayer(taxpayer)
                .taxUnit(request.taxUnitId() == null ? null : TaxUnit.builder().id(request.taxUnitId()).build())
                .finalizationReason(request.finalizationReason())
                .taxSource(request.taxSource())
                .currentState(ExecutionPaperState.UNASSIGNED.name())
                .build();
        ep = executionPaperRepository.save(ep);

        // 4. Save state history
        ExecutionPaperStateHistory history = ExecutionPaperStateHistory.builder()
                .executionPaper(ep)
                .state(ExecutionPaperState.UNASSIGNED.name())
                .stateDate(LocalDate.now().toString())
                .build();
        stateHistoryRepository.save(history);
        auditLogService.record(null, "EXECUTION_PAPER_CREATED", "EXECUTION_PAPER", ep.getId().toString(), null, ep.getExecutionPaperUniqueCode());

        // 5. Create DebtInformation
        DebtPaperIssuanceRequest.DebtInfo di = request.debtInfo();
        DebtInformation debtInfo = DebtInformation.builder()
                .executionPaper(ep)
                .taxAmount(orZero(di.taxAmount()))
                .dutyAmount(orZero(di.dutyAmount()))
                .penaltyArticle22(orZero(di.penaltyArticle22()))
                .penaltyArticle23(orZero(di.penaltyArticle23()))
                .deductiblePenalty(orZero(di.deductiblePenalty()))
                .nonDeductiblePenalty(orZero(di.nonDeductiblePenalty()))
                .taxPaidAmount(orZero(di.taxPaidAmount()))
                .penaltyPaidAmount(orZero(di.penaltyPaidAmount()))
                .taxCredit(orZero(di.taxCredit()))
                .remainingTax(orZero(di.remainingTax()))
                .municipalityShare(orZero(di.municipalityShare()))
                .guildShare(orZero(di.guildShare()))
                .complianceBonus(orZero(di.complianceBonus()))
                .build();
        debtInformationRepository.save(debtInfo);

        log.info("Created ExecutionPaper id={}, code={}", ep.getId(), ep.getExecutionPaperUniqueCode());
        return ep;
    }

    public ExecutionPaperPageResponse getAll(int page, int size) {
        return search(null, ExecutionPaperSearchCriteria.empty(), page, size);
    }

    public ExecutionPaperPageResponse searchAll(ExecutionPaperSearchCriteria criteria, int page, int size) {
        return search(null, criteria, page, size);
    }

    public ExecutionPaperPageResponse searchAssigned(ExecutionPaperSearchCriteria criteria, int page, int size) {
        return search("ASSIGNED", criteria, page, size);
    }

    public ExecutionPaperPageResponse searchUnassigned(ExecutionPaperSearchCriteria criteria, int page, int size) {
        return search("UNASSIGNED", criteria, page, size);
    }

    public ExecutionPaperPageResponse getAll(int page, int size, ExecutionPaperSearchCriteria criteria) {
        return search(null, criteria, page, size);
    }

    public ExecutionPaperPageResponse getAssigned(int page, int size, ExecutionPaperSearchCriteria criteria) {
        return search("ASSIGNED", criteria, page, size);
    }

    public ExecutionPaperPageResponse getUnassigned(int page, int size, ExecutionPaperSearchCriteria criteria) {
        return search("UNASSIGNED", criteria, page, size);
    }

    private ExecutionPaperPageResponse search(String assignmentMode, ExecutionPaperSearchCriteria criteria, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        ExecutionPaperSearchCriteria normalized = normalize(criteria);
        Page<ExecutionPaper> pageResult = executionPaperRepository.search(
                assignmentMode,
                ExecutionPaperState.UNASSIGNED.name(),
                normalized.uniqueCode(),
                normalized.taxpayerName(),
                normalized.taxpayerNationalId(),
                normalized.currentState(),
                normalized.notificationStatus(),
                normalized.taxUnitId(),
                normalized.minDebt(),
                normalized.maxDebt(),
                pageable
        );
        return toPageResponse(pageResult);
    }

    public ExecutionPaperPageResponse getAssigned(int page, int size) {
        return search("ASSIGNED", ExecutionPaperSearchCriteria.empty(), page, size);
    }

    public ExecutionPaperPageResponse getUnassigned(int page, int size) {
        return search("UNASSIGNED", ExecutionPaperSearchCriteria.empty(), page, size);
    }

    private ExecutionPaperSearchCriteria normalize(ExecutionPaperSearchCriteria criteria) {
        if (criteria == null) {
            return ExecutionPaperSearchCriteria.empty();
        }
        return new ExecutionPaperSearchCriteria(
                blankToNull(criteria.uniqueCode()),
                blankToNull(criteria.taxpayerName()),
                blankToNull(criteria.taxpayerNationalId()),
                blankToNull(criteria.currentState()),
                blankToNull(criteria.notificationStatus()),
                criteria.taxUnitId(),
                criteria.minDebt(),
                criteria.maxDebt()
        );
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }


    public ExecutionPaperDetailResponse getDetails(Long id) {
        ExecutionPaper ep = executionPaperRepository.findById(id)
                .orElseThrow(() -> new com.mparsict.vosolmaliat.common.exception.BusinessException("EXECUTION_PAPER_NOT_FOUND", "برگ اجرا یافت نشد"));

        Map<String, Object> execution = new LinkedHashMap<>();
        execution.put("id", ep.getId());
        execution.put("uniqueCode", ep.getExecutionPaperUniqueCode());
        execution.put("currentState", ep.getCurrentState());
        execution.put("currentStateLabel", ExecutionPaperState.labelOf(ep.getCurrentState()));
        execution.put("currentStateDisplay", ExecutionPaperState.labelOf(ep.getCurrentState()));
        execution.put("taxUnitId", ep.getTaxUnitId());
        putOrganizationNamesFromTaxUnit(execution, ep.getTaxUnitId());
        execution.put("finalizationReason", ep.getFinalizationReason());
        execution.put("taxSource", ep.getTaxSource());
        execution.put("taxSourceLabel", TaxSourceType.labelOf(ep.getTaxSource()));
        execution.put("taxSourceDisplay", TaxSourceType.displayOf(ep.getTaxSource()));
        execution.put("createdAt", ep.getCreatedAt());

        Map<String, Object> finalized = new LinkedHashMap<>();
        finalizedDebtPaperRepository.findById(ep.getFinalizedDebtPaperId()).ifPresent(fdp -> {
            finalized.put("debtPaperUniqueCode", fdp.getDebtPaperUniqueCode());
            finalized.put("debtPaperIssueDate", fdp.getDebtPaperIssueDate());
            finalized.put("notificationId", fdp.getNotificationId());
            finalized.put("finalizationReason", fdp.getFinalizationReason());
            finalized.put("taxSource", fdp.getTaxSource());
            finalized.put("taxSourceLabel", TaxSourceType.labelOf(fdp.getTaxSource()));
            finalized.put("taxSourceDisplay", TaxSourceType.displayOf(fdp.getTaxSource()));
            finalized.put("performanceYear", fdp.getPerformanceYear());
            finalized.put("performancePeriod", fdp.getPerformancePeriod());
            finalized.put("activityTypeCode", fdp.getActivityTypeCode());
            finalized.put("activityTypeTitle", fdp.getActivityTypeTitle());
            finalized.put("activityTypeDisplay", activityTypeDisplay(fdp.getActivityTypeTitle(), fdp.getActivityTypeCode()));
            finalized.put("mainTaxBureauId", fdp.getMainTaxBureauId());
            finalized.put("taxBureauId", fdp.getTaxBureauId());
            finalized.put("taxUnitId", fdp.getTaxUnitId());
            putOrganizationNames(finalized, fdp.getMainTaxBureauId(), fdp.getTaxBureauId(), fdp.getTaxUnitId());
            finalized.put("totalFinalizedAmount", fdp.getTotalFinalizedAmount());
            finalized.put("finalizedTaxAndNonDeductibleAmount", fdp.getFinalizedTaxAndNonDeductibleAmount());
            finalized.put("createdAt", fdp.getCreatedAt());
        });

        Map<String, Object> taxpayerMap = new LinkedHashMap<>();
        taxpayerRepository.findById(ep.getTaxpayerId()).ifPresent(taxpayer -> {
            taxpayerMap.put("id", taxpayer.getId());
            taxpayerMap.put("type", taxpayer.getType().name());
            taxpayerMap.put("typeLabel", TaxpayerType.labelOf(taxpayer.getType().name()));
            taxpayerMap.put("typeDisplay", TaxpayerType.labelOf(taxpayer.getType().name()));
            taxpayerMap.put("debtPaperUniqueCode", taxpayer.getDebtPaperUniqueCode());
            taxpayerMap.put("debtPaperIssueDate", taxpayer.getDebtPaperIssueDate());
            findContactInfo(taxpayer).ifPresent(contactInfo -> {
                taxpayerMap.put("postalCode", contactInfo.getPostalCode());
                taxpayerMap.put("address", contactInfo.getLegalAddress());
            });
            if (taxpayer.getNaturalPersonId() != null) {
                naturalPersonRepository.findById(taxpayer.getNaturalPersonId()).ifPresent(np -> {
                    taxpayerMap.put("nationalCode", np.getNationalCode());
                    taxpayerMap.put("firstName", np.getFirstName());
                    taxpayerMap.put("lastName", np.getLastName());
                    taxpayerMap.put("fatherName", np.getFatherName());
                    taxpayerMap.put("birthDate", np.getBirthDate());
                });
            } else if (taxpayer.getLegalPersonId() != null) {
                legalPersonRepository.findById(taxpayer.getLegalPersonId()).ifPresent(lp -> {
                    taxpayerMap.put("nationalId", lp.getNationalId());
                    taxpayerMap.put("registrationNumber", lp.getRegistrationNumber());
                    taxpayerMap.put("registrationDate", lp.getRegistrationDate());
                    taxpayerMap.put("companyName", lp.getName());
                });
            }
        });

        Map<String, Object> debt = new LinkedHashMap<>();
        debtInformationRepository.findByExecutionPaperId(ep.getId()).ifPresent(di -> {
            debt.put("taxAmount", di.getTaxAmount());
            debt.put("dutyAmount", di.getDutyAmount());
            debt.put("penaltyArticle22", di.getPenaltyArticle22());
            debt.put("penaltyArticle23", di.getPenaltyArticle23());
            debt.put("deductiblePenalty", di.getDeductiblePenalty());
            debt.put("nonDeductiblePenalty", di.getNonDeductiblePenalty());
            debt.put("taxPaidAmount", di.getTaxPaidAmount());
            debt.put("penaltyPaidAmount", di.getPenaltyPaidAmount());
            debt.put("taxCredit", di.getTaxCredit());
            debt.put("remainingTax", di.getRemainingTax());
            debt.put("totalDebt", di.getTotalDebt());
        });

        Map<String, Object> notification = new LinkedHashMap<>();
        executionPaperNotificationRepository.findByExecutionPaperId(ep.getId()).ifPresent(n -> {
            notification.put("electronicNotificationDate", n.getElectronicNotificationDate());
            notification.put("informStatus", n.getInformStatus());
            notification.put("informStatusLabel", ExecutionPaperState.labelOf(n.getInformStatus()));
            notification.put("informRejectionReason", n.getInformRejectionReason());
            notification.put("informRejectedAt", n.getInformRejectedAt());
            notification.put("reInformDate", n.getReInformDate());
            notification.put("nextDebtInquiryDate", n.getNextDebtInquiryDate());
            notification.put("createdAt", n.getCreatedAt());
        });

        Map<String, Object> ruleRejection = new LinkedHashMap<>();
        String debtPaperUniqueCode = stringValue(finalized.get("debtPaperUniqueCode"));
        if (debtPaperUniqueCode != null) {
            issuanceRequestRepository.findByDebtPaperUniqueCode(debtPaperUniqueCode)
                    .filter(request -> request.getRejectionReason() != null && !request.getRejectionReason().isBlank())
                    .ifPresent(request -> {
                        ruleRejection.put("rejectionType", "ISSUANCE_RULE");
                        ruleRejection.put("rejectionTypeLabel", "رد صدور برگ اجرا توسط موتور قوانین");
                        ruleRejection.put("rejectionReason", request.getRejectionReason());
                        ruleRejection.put("rejectionReasonId", request.getRejectionReasonId());
                        ruleRejection.put("rejectedAt", request.getUpdatedAt() != null ? request.getUpdatedAt() : request.getCreatedAt());
                    });
        }
        if (notification.get("informRejectionReason") != null) {
            ruleRejection.put("informRejectionType", "INFORM_RULE");
            ruleRejection.put("informRejectionTypeLabel", "رد ابلاغ برگ اجرا توسط موتور قوانین");
            ruleRejection.put("informRejectionReason", notification.get("informRejectionReason"));
            ruleRejection.put("informRejectedAt", notification.get("informRejectedAt"));
            ruleRejection.put("reInformDate", notification.get("reInformDate"));
        }

        return new ExecutionPaperDetailResponse(toDto(ep), execution, finalized, taxpayerMap, debt, notification, ruleRejection);
    }

    private String stringValue(Object value) {
        return value == null || value.toString().isBlank() ? null : value.toString();
    }

    private void putOrganizationNames(Map<String, Object> target, Long generalId, Long officeId, Long unitId) {
        if (generalId != null) {
            generalDirectorateRepository.findById(generalId).ifPresent(general -> {
                target.put("mainTaxBureauName", general.getGeneralDirectorateName());
                target.put("generalDirectorateName", general.getGeneralDirectorateName());
                target.put("generalDirectorateAddress", general.getAddress());
                target.put("generalDirectoratePostalCode", general.getPostalCode());
                target.put("generalDirectoratePhone", general.getPhone());
            });
        }
        if (officeId != null) {
            taxAffairsOfficeRepository.findById(officeId).ifPresent(office -> {
                target.put("taxBureauName", office.getOfficeName());
                target.put("taxAffairsOfficeName", office.getOfficeName());
                target.put("taxAffairsOfficeAddress", office.getAddress());
                target.put("taxAffairsOfficePostalCode", office.getPostalCode());
                target.put("taxAffairsOfficePhone", office.getPhone());
            });
        }
        if (unitId != null) {
            taxUnitRepository.findById(unitId).ifPresent(unit -> {
                target.put("taxUnitName", unit.getTaxUnitName());
                target.put("taxUnitAddress", unit.getAddress());
                target.put("taxUnitPostalCode", unit.getPostalCode());
                target.put("taxUnitPhone", unit.getPhone());
            });
        }
    }

    private String activityTypeDisplay(String title, String code) {
        boolean hasTitle = title != null && !title.isBlank();
        boolean hasCode = code != null && !code.isBlank();
        if (hasTitle && hasCode) {
            return title + " (" + code + ")";
        }
        if (hasTitle) {
            return title;
        }
        return hasCode ? code : null;
    }

    private void putOrganizationNamesFromTaxUnit(Map<String, Object> target, Long unitId) {
        if (unitId == null) {
            return;
        }
        taxUnitRepository.findById(unitId).ifPresent(unit -> {
            target.put("taxUnitName", unit.getTaxUnitName());
            target.put("taxUnitAddress", unit.getAddress());
            target.put("taxUnitPostalCode", unit.getPostalCode());
            target.put("taxUnitPhone", unit.getPhone());
            if (unit.getTaxAffairsOfficeId() != null) {
                taxAffairsOfficeRepository.findById(unit.getTaxAffairsOfficeId()).ifPresent(office -> {
                    target.put("taxBureauId", office.getId());
                    target.put("taxBureauName", office.getOfficeName());
                    target.put("taxAffairsOfficeName", office.getOfficeName());
                    target.put("taxAffairsOfficeAddress", office.getAddress());
                    target.put("taxAffairsOfficePostalCode", office.getPostalCode());
                    target.put("taxAffairsOfficePhone", office.getPhone());
                    if (office.getGeneralDirectorateOfTaxAffairsId() != null) {
                        generalDirectorateRepository.findById(office.getGeneralDirectorateOfTaxAffairsId()).ifPresent(general -> {
                            target.put("mainTaxBureauId", general.getId());
                            target.put("mainTaxBureauName", general.getGeneralDirectorateName());
                            target.put("generalDirectorateName", general.getGeneralDirectorateName());
                            target.put("generalDirectorateAddress", general.getAddress());
                            target.put("generalDirectoratePostalCode", general.getPostalCode());
                            target.put("generalDirectoratePhone", general.getPhone());
                        });
                    }
                });
            }
        });
    }

    private void upsertContactInfo(DebtPaperIssuanceRequest.TaxpayerInfo taxpayerInfo, Long naturalPersonId, Long legalPersonId) {
        if (!hasText(taxpayerInfo.postalCode()) && !hasText(taxpayerInfo.address())) {
            return;
        }
        ContactInfo contactInfo = naturalPersonId != null
                ? contactInfoRepository.findFirstByNaturalPersonId(naturalPersonId)
                        .orElseGet(() -> ContactInfo.builder().naturalPerson(NaturalPerson.builder().id(naturalPersonId).build()).build())
                : contactInfoRepository.findFirstByLegalPersonId(legalPersonId)
                        .orElseGet(() -> ContactInfo.builder().legalPerson(LegalPerson.builder().id(legalPersonId).build()).build());
        if (hasText(taxpayerInfo.postalCode())) {
            contactInfo.setPostalCode(taxpayerInfo.postalCode());
        }
        if (hasText(taxpayerInfo.address())) {
            contactInfo.setLegalAddress(taxpayerInfo.address());
        }
        contactInfoRepository.save(contactInfo);
    }

    private Optional<ContactInfo> findContactInfo(Taxpayer taxpayer) {
        if (taxpayer.getNaturalPersonId() != null) {
            return contactInfoRepository.findFirstByNaturalPersonId(taxpayer.getNaturalPersonId());
        }
        if (taxpayer.getLegalPersonId() != null) {
            return contactInfoRepository.findFirstByLegalPersonId(taxpayer.getLegalPersonId());
        }
        return Optional.empty();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public ExecutionPaperTraceResponse getTrace(Long id) {
        ExecutionPaper ep = executionPaperRepository.findById(id)
                .orElseThrow(() -> new com.mparsict.vosolmaliat.common.exception.BusinessException("EXECUTION_PAPER_NOT_FOUND", "برگ اجرا یافت نشد"));

        List<ExecutionPaperTraceItemResponse> stateItems = stateHistoryRepository.findByExecutionPaperId(id).stream()
                .map(history -> new ExecutionPaperTraceItemResponse(
                        "STATE",
                        history.getState(),
                        stateLabel(history.getState()),
                        history.getCreatedBy() == null ? "system" : history.getCreatedBy(),
                        null,
                        history.getState(),
                        stateHistoryDate(history)
                ))
                .toList();

        List<ExecutionPaperTraceItemResponse> auditItems = auditLogRepository
                .findByTargetTypeAndTargetIdOrderByCreatedAtAsc("EXECUTION_PAPER", id.toString())
                .stream()
                .map(this::toTraceItem)
                .toList();

        List<ExecutionPaperTraceItemResponse> items = java.util.stream.Stream.concat(stateItems.stream(), auditItems.stream())
                .sorted(Comparator.comparing(ExecutionPaperTraceItemResponse::occurredAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        return new ExecutionPaperTraceResponse(id, ep.getExecutionPaperUniqueCode(), items);
    }

    private ExecutionPaperTraceItemResponse toTraceItem(AuditLog auditLog) {
        return new ExecutionPaperTraceItemResponse(
                "AUDIT",
                auditLog.getAction(),
                actionLabel(auditLog.getAction()),
                auditLog.getActorUsername(),
                auditLog.getOldValue(),
                auditLog.getNewValue(),
                auditLog.getCreatedAt()
        );
    }

    private LocalDateTime stateHistoryDate(ExecutionPaperStateHistory history) {
        if (history.getCreatedAt() != null) {
            return history.getCreatedAt();
        }
        try {
            return LocalDate.parse(history.getStateDate()).atTime(LocalTime.MIN);
        } catch (Exception ex) {
            return null;
        }
    }

    private String stateLabel(String state) {
        return switch (state == null ? "" : state) {
            case "UNASSIGNED" -> "ایجاد برگ اجرا و ورود به صف ارجاع";
            case "ASSIGNED" -> "اختصاص برگ اجرا";
            case "INFORM_SENT" -> "ارسال برگ اجرا به سامانه ابلاغ الکترونیک";
            case "INFORMED" -> "دریافت تاریخ ابلاغ";
            case "INFORM_REJECTED" -> "رد ابلاغ براساس قوانین";
            case "NOTIFIED" -> "ابلاغ برگ اجرا";
            case "COMPLETED" -> "تکمیل فرایند برگ اجرا";
            case "CANCELLED" -> "لغو برگ اجرا";
            default -> state == null ? "تغییر وضعیت" : state;
        };
    }

    private String actionLabel(String action) {
        return switch (action == null ? "" : action) {
            case "EXECUTION_PAPER_CREATED" -> "ثبت برگ اجرا";
            case "TASK_CLAIMED" -> "دریافت کار";
            case "TASK_UNCLAIMED" -> "رهاسازی کار";
            case "TASK_COMPLETED" -> "تکمیل کار";
            case "TASK_ASSIGNED" -> "اختصاص کار";
            case "TASK_DELEGATED_TO_USER" -> "ارجاع به کاربر";
            case "TASK_DELEGATED_TO_GROUP" -> "ارجاع به نقش بعدی";
            case "INFORM_SENT_TO_ELECTRONIC_NOTIFICATION" -> "ارسال به سامانه ابلاغ الکترونیک";
            case "ELECTRONIC_NOTIFICATION_CONFIRMED" -> "تایید ابلاغ الکترونیک";
            case "EXECUTION_PAPER_INFORM_REJECTED" -> "رد ابلاغ برگ اجرا";
            case "EXECUTION_PAPER_INFORMED" -> "ثبت تاریخ ابلاغ";
            case "EXECUTION_PAPER_RE_INFORM_SCHEDULED" -> "زمان‌بندی ابلاغ مجدد";
            case "NEXT_DEBT_INQUIRY_DATE_SET" -> "تنظیم تاریخ استعلام بدهی";
            case "INFORM_DATE_SENT_TO_SANIM" -> "ارسال تاریخ ابلاغ به سنیم";
            case "SANIM_INFORM_DATE_CONFIRMED" -> "تایید دریافت سنیم";
            case "RELATED_PERSONS_REFRESHED_FROM_COMPANY_REGISTRY" ->
                    "بروزرسانی اشخاص مرتبط از سامانه ثبت شرکت‌ها";
            default -> action == null ? "عملیات" : action;
        };
    }

    private ExecutionPaperPageResponse toPageResponse(Page<ExecutionPaper> pageResult) {
        List<ExecutionPaperDto> content = pageResult.getContent()
                .stream()
                .map(this::toDto)
                .toList();
        return new ExecutionPaperPageResponse(
                content,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }

    private ExecutionPaperDto toDto(ExecutionPaper ep) {
        String taxpayerName = null;
        String taxpayerNationalId = null;
        String taxpayerType = null;

        Optional<Taxpayer> taxpayerOpt = taxpayerRepository.findById(ep.getTaxpayerId());
        if (taxpayerOpt.isPresent()) {
            Taxpayer taxpayer = taxpayerOpt.get();
            if (taxpayer.getNaturalPersonId() != null) {
                taxpayerType = "حقیقی";
                Optional<NaturalPerson> np = naturalPersonRepository.findById(taxpayer.getNaturalPersonId());
                if (np.isPresent()) {
                    taxpayerName = np.get().getFirstName() + " " + np.get().getLastName();
                    taxpayerNationalId = np.get().getNationalCode();
                }
            } else if (taxpayer.getLegalPersonId() != null) {
                taxpayerType = "حقوقی";
                Optional<LegalPerson> lp = legalPersonRepository.findById(taxpayer.getLegalPersonId());
                if (lp.isPresent()) {
                    taxpayerName = lp.get().getName();
                    taxpayerNationalId = lp.get().getNationalId();
                }
            }
        }

        // Resolve notification info
        String notificationDate = null;
        String notificationStatus = "NOT_NOTIFIED";
        String paymentDeadlineStatus = null;

        Optional<ExecutionPaperNotification> notifOpt =
                executionPaperNotificationRepository.findByExecutionPaperId(ep.getId());
        if (notifOpt.isPresent()) {
            ExecutionPaperNotification notif = notifOpt.get();
            notificationDate = notif.getElectronicNotificationDate();
            if (notificationDate != null) {
                notificationStatus = "NOTIFIED";
                try {
                    LocalDate notifLocalDate = LocalDate.parse(notificationDate.substring(0, 10));
                    long daysSince = java.time.temporal.ChronoUnit.DAYS.between(notifLocalDate, LocalDate.now());
                    paymentDeadlineStatus = daysSince > 30 ? "DEADLINE_EXPIRED" : "AWAITING_PAYMENT";
                } catch (Exception e) {
                    log.warn("Could not parse notification date: {}", notificationDate);
                }
            }
        }

        // Resolve total debt
        Double totalDebt = null;
        Optional<DebtInformation> debtOpt = debtInformationRepository.findByExecutionPaperId(ep.getId());
        if (debtOpt.isPresent()) {
            totalDebt = debtOpt.get().getTotalDebt();
        }

        return new ExecutionPaperDto(
                ep.getId(),
                ep.getExecutionPaperUniqueCode(),
                taxpayerName,
                taxpayerNationalId,
                taxpayerType,
                notificationDate,
                paymentDeadlineStatus,
                totalDebt,
                notificationStatus,
                ep.getCurrentState()
        );
    }

    private NaturalPerson resolveNaturalPerson(DebtPaperIssuanceRequest.TaxpayerInfo info) {
        if (info.nationalCode() != null) {
            Optional<NaturalPerson> existing = naturalPersonRepository.findByNationalCode(info.nationalCode());
            if (existing.isPresent()) return existing.get();
        }
        if (info.fida() != null) {
            Optional<NaturalPerson> existing = naturalPersonRepository.findByFida(info.fida());
            if (existing.isPresent()) return existing.get();
        }
        NaturalPerson np = NaturalPerson.builder()
                .nationalCode(info.nationalCode())
                .fida(info.fida())
                .firstName(info.firstName() != null ? info.firstName() : "")
                .lastName(info.lastName() != null ? info.lastName() : "")
                .fatherName(info.fatherName() != null ? info.fatherName() : "")
                .birthDate(info.birthDate() != null ? info.birthDate() : "")
                .build();
        return naturalPersonRepository.save(np);
    }

    private LegalPerson resolveLegalPerson(DebtPaperIssuanceRequest.TaxpayerInfo info) {
        if (info.nationalId() != null) {
            Optional<LegalPerson> existing = legalPersonRepository.findByNationalId(info.nationalId());
            if (existing.isPresent()) return existing.get();
        }
        if (info.registrationNumber() != null) {
            Optional<LegalPerson> existing = legalPersonRepository.findByRegistrationNumber(info.registrationNumber());
            if (existing.isPresent()) return existing.get();
        }
        LegalPerson lp = LegalPerson.builder()
                .nationalId(info.nationalId())
                .registrationNumber(info.registrationNumber())
                .registrationDate(info.registrationDate() != null ? info.registrationDate() : "")
                .name(info.companyName() != null ? info.companyName() : "")
                .build();
        return legalPersonRepository.save(lp);
    }

    private double computeTotalFinalizedAmount(DebtPaperIssuanceRequest.DebtInfo debtInfo) {
        if (debtInfo == null) return 0.0;
        return orZero(debtInfo.taxAmount())
                + orZero(debtInfo.penaltyArticle22())
                + orZero(debtInfo.nonDeductiblePenalty());
    }

    private double computeFinalizedTaxAndNonDeductibleAmount(DebtPaperIssuanceRequest.DebtInfo debtInfo) {
        if (debtInfo == null) return 0.0;
        return orZero(debtInfo.taxAmount()) + orZero(debtInfo.nonDeductiblePenalty());
    }

    private double orZero(Double val) {
        return val != null ? val : 0.0;
    }
}
