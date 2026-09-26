package com.mparsict.vosolmaliat.presentation.api.admin;

import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.application.dto.request.admin.ManualExecutionPaperRequest;
import com.mparsict.vosolmaliat.application.dto.response.ExecutionPaperDetailResponse;
import com.mparsict.vosolmaliat.application.dto.response.IssuanceResponse;
import com.mparsict.vosolmaliat.application.dto.response.admin.AdminDataSummaryResponse;
import com.mparsict.vosolmaliat.application.service.admin.AdminDataEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/data-entry")
@RequiredArgsConstructor
public class AdminDataEntryController {

    private final AdminDataEntryService adminDataEntryService;
    private final com.mparsict.vosolmaliat.application.service.Ef03ReceptionService ef03Reception;

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public AdminDataSummaryResponse summary() {
        return adminDataEntryService.summary();
    }

    @PostMapping("/finalized-debt-papers")
    @PreAuthorize("hasAuthority('USER_PROFILE_CREATE')")
    public org.springframework.http.ResponseEntity<?> createFinalizedDebtPaper(@RequestBody @Valid DebtPaperIssuanceRequest request) {
        if (isEf03(request)) return receiveEf03(request);
        return org.springframework.http.ResponseEntity.status(HttpStatus.CREATED)
                .body(adminDataEntryService.createFinalizedDebtPaperAndExecutionPaper(request));
    }

    @PostMapping("/finalized-debt-papers/send")
    @PreAuthorize("hasAuthority('USER_PROFILE_CREATE')")
    public org.springframework.http.ResponseEntity<?> sendFinalizedDebtPaper(@RequestBody @Valid DebtPaperIssuanceRequest request) {
        if (isEf03(request)) return receiveEf03(request);
        return org.springframework.http.ResponseEntity.ok(adminDataEntryService.sendFinalizedDebtPaperToWorkflow(request));
    }

    private boolean isEf03(DebtPaperIssuanceRequest request) {
        return request.sourceDocument() != null && ("LTR_EF03".equals(request.sourceDocument().getTemplateCode())
                || "EF03".equals(request.sourceDocument().getTemplateCode()));
    }

    private org.springframework.http.ResponseEntity<?> receiveEf03(DebtPaperIssuanceRequest request) {
        var source = request.sourceDocument();
        if (source.getXml() == null || source.getXml().isBlank())
            throw new com.mparsict.vosolmaliat.common.exception.BusinessException("EF03_XML_REQUIRED", "XML اصلی EF03 لازم است");
        var result = ef03Reception.receive(source.getXml(), request.debtPaperUniqueCode(), source.getRegistrationTraceCode(),
                source.getSourceTaxOfficeCode(), request.taxSource());
        return org.springframework.http.ResponseEntity.status(result.executionPaperId() == null ? 202 : 200).body(result);
    }

    @PostMapping("/execution-papers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('USER_PROFILE_CREATE')")
    public ExecutionPaperDetailResponse createExecutionPaper(@RequestBody @Valid ManualExecutionPaperRequest request) {
        return adminDataEntryService.createManualExecutionPaper(request);
    }
}
