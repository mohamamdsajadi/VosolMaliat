package com.mparsict.vosolmaliat.presentation.api;

import com.mparsict.vosolmaliat.application.dto.request.*;
import com.mparsict.vosolmaliat.application.dto.response.*;
import com.mparsict.vosolmaliat.application.service.SanimAuditService;
import com.mparsict.vosolmaliat.domain.enums.TaxpayerType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fake-sanim/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasAuthority('AUDIT_VIEW')")
public class FakeSanimAuditController {
    private final SanimAuditService service;
    @GetMapping("/taxpayers") public List<SanimTaxpayerOptionResponse> taxpayers() { return service.taxpayers(); }
    @GetMapping("/taxpayers/{id}/history") public List<SanimAuditEntryResponse> history(@PathVariable Long id) { return service.history(id); }
    @GetMapping("/taxpayers/{id}/inquiry") public SanimDebtStatusResponse inquire(@PathVariable Long id) { return service.inquire(id); }
    @GetMapping("/inquiry") public SanimDebtStatusResponse inquire(@RequestParam TaxpayerType taxpayerType, @RequestParam String identifier) {
        return service.inquire(taxpayerType, identifier);
    }
    @PostMapping("/taxpayers/{id}/entries") @PreAuthorize("hasRole('ADMIN')")
    public SanimAuditEntryResponse add(@PathVariable Long id, @RequestBody @Valid SanimAuditEntryRequest request) { return service.add(id, request); }
    @PostMapping("/taxpayers/{id}/random") @PreAuthorize("hasRole('ADMIN')")
    public List<SanimAuditEntryResponse> random(@PathVariable Long id, @RequestBody @Valid SanimAuditRandomRequest request) {
        return service.generateRandom(id, request.count());
    }
}
