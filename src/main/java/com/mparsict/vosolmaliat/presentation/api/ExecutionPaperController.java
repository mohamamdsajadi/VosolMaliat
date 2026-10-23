package com.mparsict.vosolmaliat.presentation.api;

import com.mparsict.vosolmaliat.application.dto.request.ExecutionPaperSearchCriteria;
import com.mparsict.vosolmaliat.application.dto.response.ExecutionPaperDetailResponse;
import com.mparsict.vosolmaliat.application.dto.response.ExecutionPaperPageResponse;
import com.mparsict.vosolmaliat.application.dto.response.ExecutionPaperRelatedPersonsResponse;
import com.mparsict.vosolmaliat.application.dto.response.ExecutionPaperTraceResponse;
import com.mparsict.vosolmaliat.application.dto.response.ExternalSystemRequestResponse;
import com.mparsict.vosolmaliat.application.service.ExecutionPaperService;
import com.mparsict.vosolmaliat.application.service.ExternalSystemRequestService;
import com.mparsict.vosolmaliat.application.service.RelatedPersonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/execution-papers")
@RequiredArgsConstructor
public class ExecutionPaperController {

    private final ExecutionPaperService executionPaperService;
    private final com.mparsict.vosolmaliat.application.service.security.ExecutionPaperVisibility visibility;
    private final RelatedPersonService relatedPersonService;
    private final ExternalSystemRequestService externalSystemRequestService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('REPORT_VIEW','EXECUTION_PAPER_LIST_VIEW')")
    public ExecutionPaperPageResponse getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String uniqueCode,
            @RequestParam(required = false) String taxpayerName,
            @RequestParam(required = false) String taxpayerNationalId,
            @RequestParam(required = false) String currentState,
            @RequestParam(required = false) String notificationStatus,
            @RequestParam(required = false) java.math.BigDecimal minDebt,
            @RequestParam(required = false) java.math.BigDecimal maxDebt,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String taxpayerType,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate issuedFrom,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate issuedTo,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        ExecutionPaperSearchCriteria criteria = new ExecutionPaperSearchCriteria(
                uniqueCode, taxpayerName, taxpayerNationalId, currentState, notificationStatus, minDebt, maxDebt, search, taxpayerType, issuedFrom, issuedTo, sortBy, sortDirection);
        log.debug("GET /api/v1/execution-papers page={}, size={}, criteria={}", page, size, criteria);
        return executionPaperService.getAll(page, size, criteria);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('REPORT_VIEW','EXECUTION_PAPER_DETAIL_VIEW')")
    public ExecutionPaperDetailResponse getDetails(@PathVariable Long id) {
        log.debug("GET /api/v1/execution-papers/{}", id);
        return executionPaperService.getDetails(id);
    }

    @GetMapping("/{id}/trace")
    @PreAuthorize("hasAnyAuthority('REPORT_VIEW','AUDIT_VIEW','EXECUTION_PAPER_TRACE_VIEW')")
    public ExecutionPaperTraceResponse getTrace(@PathVariable Long id) {
        log.debug("GET /api/v1/execution-papers/{}/trace", id);
        return executionPaperService.getTrace(id);
    }

    @GetMapping("/{id}/external-requests")
    @PreAuthorize("hasAnyAuthority('REPORT_VIEW','EXECUTION_PAPER_DETAIL_VIEW')")
    public List<ExternalSystemRequestResponse> getExternalRequests(@PathVariable Long id) {
        visibility.requireVisible(id);
        log.debug("GET /api/v1/execution-papers/{}/external-requests", id);
        return externalSystemRequestService.findByExecutionPaperId(id);
    }

    @GetMapping("/{id}/related-persons")
    @PreAuthorize("hasAnyAuthority('REPORT_VIEW','EXECUTION_PAPER_DETAIL_VIEW')")
    public ExecutionPaperRelatedPersonsResponse getRelatedPersons(@PathVariable Long id) {
        visibility.requireVisible(id);
        log.debug("GET /api/v1/execution-papers/{}/related-persons", id);
        return relatedPersonService.getForExecutionPaper(id);
    }

    @PostMapping("/{id}/related-persons/refresh")
    @PreAuthorize("hasAnyAuthority('REPORT_VIEW','EXECUTION_PAPER_DETAIL_VIEW')")
    public ExecutionPaperRelatedPersonsResponse refreshRelatedPersons(@PathVariable Long id) {
        visibility.requireVisible(id);
        log.info("POST /api/v1/execution-papers/{}/related-persons/refresh", id);
        return relatedPersonService.refreshForExecutionPaper(id);
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasAnyAuthority('REPORT_VIEW','EXECUTION_PAPER_LIST_VIEW')")
    public ExecutionPaperPageResponse getAssigned(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String uniqueCode,
            @RequestParam(required = false) String taxpayerName,
            @RequestParam(required = false) String taxpayerNationalId,
            @RequestParam(required = false) String currentState,
            @RequestParam(required = false) String notificationStatus,
            @RequestParam(required = false) java.math.BigDecimal minDebt,
            @RequestParam(required = false) java.math.BigDecimal maxDebt,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String taxpayerType,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate issuedFrom,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate issuedTo,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        ExecutionPaperSearchCriteria criteria = new ExecutionPaperSearchCriteria(
                uniqueCode, taxpayerName, taxpayerNationalId, currentState, notificationStatus, minDebt, maxDebt, search, taxpayerType, issuedFrom, issuedTo, sortBy, sortDirection);
        log.debug("GET /api/v1/execution-papers/assigned page={}, size={}, criteria={}", page, size, criteria);
        return executionPaperService.getAssigned(page, size, criteria);
    }

    @GetMapping("/unassigned")
    @PreAuthorize("hasAnyAuthority('REPORT_VIEW','EXECUTION_PAPER_LIST_VIEW')")
    public ExecutionPaperPageResponse getUnassigned(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String uniqueCode,
            @RequestParam(required = false) String taxpayerName,
            @RequestParam(required = false) String taxpayerNationalId,
            @RequestParam(required = false) String currentState,
            @RequestParam(required = false) String notificationStatus,
            @RequestParam(required = false) java.math.BigDecimal minDebt,
            @RequestParam(required = false) java.math.BigDecimal maxDebt,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String taxpayerType,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate issuedFrom,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate issuedTo,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        ExecutionPaperSearchCriteria criteria = new ExecutionPaperSearchCriteria(
                uniqueCode, taxpayerName, taxpayerNationalId, currentState, notificationStatus, minDebt, maxDebt, search, taxpayerType, issuedFrom, issuedTo, sortBy, sortDirection);
        log.debug("GET /api/v1/execution-papers/unassigned page={}, size={}, criteria={}", page, size, criteria);
        return executionPaperService.getUnassigned(page, size, criteria);
    }
}
