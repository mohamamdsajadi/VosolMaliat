package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.application.dto.response.ExecutionPaperDetailResponse;
import com.mparsict.vosolmaliat.application.dto.response.ExecutionPaperDto;
import com.mparsict.vosolmaliat.application.dto.response.ExecutionPaperPageResponse;
import com.mparsict.vosolmaliat.common.util.UniqueCodeGenerator;
import com.mparsict.vosolmaliat.domain.enums.ExecutionPaperState;
import com.mparsict.vosolmaliat.domain.model.Taxpayer;
import com.mparsict.vosolmaliat.domain.model.execution.*;
import com.mparsict.vosolmaliat.domain.model.person.LegalPerson;
import com.mparsict.vosolmaliat.domain.model.person.NaturalPerson;
import com.mparsict.vosolmaliat.domain.repository.*;
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
    private final NaturalPersonRepository naturalPersonRepository;
    private final LegalPersonRepository legalPersonRepository;
    private final DebtInformationRepository debtInformationRepository;
    private final ExecutionPaperStateHistoryRepository stateHistoryRepository;
    private final ExecutionPaperNotificationRepository executionPaperNotificationRepository;
    private final UniqueCodeGenerator uniqueCodeGenerator;

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
                                .mainTaxBureauId(request.mainTaxBureauId())
                                .taxBureauId(request.taxBureauId())
                                .taxUnitId(request.taxUnitId())
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
                                .naturalPersonId(finalNaturalPersonId)
                                .legalPersonId(finalLegalPersonId)
                                .build()
                ));

        // 3. Create ExecutionPaper with UNASSIGNED state
        String epCode = uniqueCodeGenerator.generateExecutionPaperCode();
        ExecutionPaper ep = ExecutionPaper.builder()
                .executionPaperUniqueCode(epCode)
                .finalizedDebtPaperId(fdp.getId())
                .taxpayerId(taxpayer.getId())
                .mainTaxBureauId(request.mainTaxBureauId())
                .taxBureauId(request.taxBureauId())
                .taxUnitId(request.taxUnitId())
                .sendingUnitId(request.sendingUnitId())
                .finalizationReason(request.finalizationReason())
                .taxSource(request.taxSource())
                .currentState(ExecutionPaperState.UNASSIGNED.name())
                .build();
        ep = executionPaperRepository.save(ep);

        // 4. Save state history
        ExecutionPaperStateHistory history = ExecutionPaperStateHistory.builder()
                .executionPaperId(ep.getId())
                .state(ExecutionPaperState.UNASSIGNED.name())
                .stateDate(LocalDate.now().toString())
                .build();
        stateHistoryRepository.save(history);

        // 5. Create DebtInformation
        DebtPaperIssuanceRequest.DebtInfo di = request.debtInfo();
        DebtInformation debtInfo = DebtInformation.builder()
                .executionPaperId(ep.getId())
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
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<ExecutionPaper> pageResult = executionPaperRepository.findAll(pageable);
        return toPageResponse(pageResult);
    }

    public ExecutionPaperPageResponse getAssigned(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<ExecutionPaper> pageResult = executionPaperRepository
                .findByCurrentStateNot(ExecutionPaperState.UNASSIGNED.name(), pageable);
        return toPageResponse(pageResult);
    }

    public ExecutionPaperPageResponse getUnassigned(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<ExecutionPaper> pageResult = executionPaperRepository
                .findByCurrentState(ExecutionPaperState.UNASSIGNED.name(), pageable);
        return toPageResponse(pageResult);
    }


    public ExecutionPaperDetailResponse getDetails(Long id) {
        ExecutionPaper ep = executionPaperRepository.findById(id)
                .orElseThrow(() -> new com.mparsict.vosolmaliat.common.exception.BusinessException("EXECUTION_PAPER_NOT_FOUND", "برگ اجرا یافت نشد"));

        Map<String, Object> execution = new LinkedHashMap<>();
        execution.put("id", ep.getId());
        execution.put("uniqueCode", ep.getExecutionPaperUniqueCode());
        execution.put("currentState", ep.getCurrentState());
        execution.put("mainTaxBureauId", ep.getMainTaxBureauId());
        execution.put("taxBureauId", ep.getTaxBureauId());
        execution.put("taxUnitId", ep.getTaxUnitId());
        execution.put("sendingUnitId", ep.getSendingUnitId());
        execution.put("finalizationReason", ep.getFinalizationReason());
        execution.put("taxSource", ep.getTaxSource());
        execution.put("createdAt", ep.getCreatedAt());

        Map<String, Object> finalized = new LinkedHashMap<>();
        finalizedDebtPaperRepository.findById(ep.getFinalizedDebtPaperId()).ifPresent(fdp -> {
            finalized.put("debtPaperUniqueCode", fdp.getDebtPaperUniqueCode());
            finalized.put("debtPaperIssueDate", fdp.getDebtPaperIssueDate());
            finalized.put("notificationId", fdp.getNotificationId());
            finalized.put("finalizationReason", fdp.getFinalizationReason());
            finalized.put("taxSource", fdp.getTaxSource());
            finalized.put("performanceYear", fdp.getPerformanceYear());
            finalized.put("performancePeriod", fdp.getPerformancePeriod());
            finalized.put("activityTypeCode", fdp.getActivityTypeCode());
            finalized.put("mainTaxBureauId", fdp.getMainTaxBureauId());
            finalized.put("taxBureauId", fdp.getTaxBureauId());
            finalized.put("taxUnitId", fdp.getTaxUnitId());
            finalized.put("totalFinalizedAmount", fdp.getTotalFinalizedAmount());
            finalized.put("finalizedTaxAndNonDeductibleAmount", fdp.getFinalizedTaxAndNonDeductibleAmount());
            finalized.put("createdAt", fdp.getCreatedAt());
        });

        Map<String, Object> taxpayerMap = new LinkedHashMap<>();
        taxpayerRepository.findById(ep.getTaxpayerId()).ifPresent(taxpayer -> {
            taxpayerMap.put("id", taxpayer.getId());
            taxpayerMap.put("type", taxpayer.getType().name());
            taxpayerMap.put("debtPaperUniqueCode", taxpayer.getDebtPaperUniqueCode());
            taxpayerMap.put("debtPaperIssueDate", taxpayer.getDebtPaperIssueDate());
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
            notification.put("createdAt", n.getCreatedAt());
        });

        return new ExecutionPaperDetailResponse(toDto(ep), execution, finalized, taxpayerMap, debt, notification);
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
