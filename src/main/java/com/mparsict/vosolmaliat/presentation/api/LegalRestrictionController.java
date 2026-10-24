package com.mparsict.vosolmaliat.presentation.api;

import com.mparsict.vosolmaliat.application.dto.response.LegalRestrictionResponses.*;
import com.mparsict.vosolmaliat.application.service.LegalRestrictionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController @RequestMapping("/api/v1/legal-restrictions/article-186") @RequiredArgsConstructor
public class LegalRestrictionController {
    private final LegalRestrictionQueryService service;
    @GetMapping public Page list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "") String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.list(page, size, search, status, from, to);
    }
    @GetMapping("/{id}") public Detail detail(@PathVariable Long id) { return service.detail(id); }
}
