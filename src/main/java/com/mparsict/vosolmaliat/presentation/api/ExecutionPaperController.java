package com.mparsict.vosolmaliat.presentation.api;

import com.mparsict.vosolmaliat.application.dto.response.ExecutionPaperDetailResponse;
import com.mparsict.vosolmaliat.application.dto.response.ExecutionPaperPageResponse;
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
            @RequestParam(defaultValue = "10") int size) {
        log.debug("GET /api/v1/execution-papers page={}, size={}", page, size);
        return executionPaperService.getAll(page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ExecutionPaperDetailResponse getDetails(@PathVariable Long id) {
        log.debug("GET /api/v1/execution-papers/{}", id);
        return executionPaperService.getDetails(id);
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ExecutionPaperPageResponse getAssigned(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("GET /api/v1/execution-papers/assigned page={}, size={}", page, size);
        return executionPaperService.getAssigned(page, size);
    }

    @GetMapping("/unassigned")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ExecutionPaperPageResponse getUnassigned(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("GET /api/v1/execution-papers/unassigned page={}, size={}", page, size);
        return executionPaperService.getUnassigned(page, size);
    }
}
