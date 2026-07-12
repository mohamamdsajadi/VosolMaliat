package com.mparsict.vosolmaliat.presentation.api;

import com.mparsict.vosolmaliat.application.dto.request.ExecutionPaperSearchCriteria;
import com.mparsict.vosolmaliat.application.dto.response.ExecutionPaperDetailResponse;
import com.mparsict.vosolmaliat.application.dto.response.ExecutionPaperPageResponse;
import com.mparsict.vosolmaliat.application.dto.response.ExecutionPaperTraceResponse;
import com.mparsict.vosolmaliat.application.service.ExecutionPaperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/execution-papers")
@RequiredArgsConstructor
public class ExecutionPaperController {

    private final ExecutionPaperService executionPaperService;

    @GetMapping
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ExecutionPaperPageResponse getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String uniqueCode,
            @RequestParam(required = false) String taxpayerName,
            @RequestParam(required = false) String taxpayerNationalId,
            @RequestParam(required = false) String currentState,
            @RequestParam(required = false) String notificationStatus,
            @RequestParam(required = false) Long taxUnitId,
            @RequestParam(required = false) Double minDebt,
            @RequestParam(required = false) Double maxDebt) {
        ExecutionPaperSearchCriteria criteria = new ExecutionPaperSearchCriteria(
                uniqueCode, taxpayerName, taxpayerNationalId, currentState, notificationStatus, taxUnitId, minDebt, maxDebt);
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

    @GetMapping("/assigned")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ExecutionPaperPageResponse getAssigned(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String uniqueCode,
            @RequestParam(required = false) String taxpayerName,
            @RequestParam(required = false) String taxpayerNationalId,
            @RequestParam(required = false) String currentState,
            @RequestParam(required = false) String notificationStatus,
            @RequestParam(required = false) Long taxUnitId,
            @RequestParam(required = false) Double minDebt,
            @RequestParam(required = false) Double maxDebt) {
        ExecutionPaperSearchCriteria criteria = new ExecutionPaperSearchCriteria(
                uniqueCode, taxpayerName, taxpayerNationalId, currentState, notificationStatus, taxUnitId, minDebt, maxDebt);
        log.debug("GET /api/v1/execution-papers/assigned page={}, size={}, criteria={}", page, size, criteria);
        return executionPaperService.getAssigned(page, size, criteria);
    }

    @GetMapping("/unassigned")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ExecutionPaperPageResponse getUnassigned(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String uniqueCode,
            @RequestParam(required = false) String taxpayerName,
            @RequestParam(required = false) String taxpayerNationalId,
            @RequestParam(required = false) String currentState,
            @RequestParam(required = false) String notificationStatus,
            @RequestParam(required = false) Long taxUnitId,
            @RequestParam(required = false) Double minDebt,
            @RequestParam(required = false) Double maxDebt) {
        ExecutionPaperSearchCriteria criteria = new ExecutionPaperSearchCriteria(
                uniqueCode, taxpayerName, taxpayerNationalId, currentState, notificationStatus, taxUnitId, minDebt, maxDebt);
        log.debug("GET /api/v1/execution-papers/unassigned page={}, size={}, criteria={}", page, size, criteria);
        return executionPaperService.getUnassigned(page, size, criteria);
    }
}
