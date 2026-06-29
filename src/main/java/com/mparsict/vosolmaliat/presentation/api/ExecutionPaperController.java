package com.mparsict.vosolmaliat.presentation.api;

import com.mparsict.vosolmaliat.application.dto.response.ExecutionPaperPageResponse;
import com.mparsict.vosolmaliat.application.service.ExecutionPaperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/execution-papers")
@RequiredArgsConstructor
public class ExecutionPaperController {

    private final ExecutionPaperService executionPaperService;

    @GetMapping
    public ExecutionPaperPageResponse getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("GET /api/v1/execution-papers page={}, size={}", page, size);
        return executionPaperService.getAll(page, size);
    }

    @GetMapping("/assigned")
    public ExecutionPaperPageResponse getAssigned(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("GET /api/v1/execution-papers/assigned page={}, size={}", page, size);
        return executionPaperService.getAssigned(page, size);
    }

    @GetMapping("/unassigned")
    public ExecutionPaperPageResponse getUnassigned(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("GET /api/v1/execution-papers/unassigned page={}, size={}", page, size);
        return executionPaperService.getUnassigned(page, size);
    }
}
