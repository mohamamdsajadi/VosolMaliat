package com.mparsict.vosolmaliat.presentation.api;

import com.mparsict.vosolmaliat.application.dto.response.FakeSanimDebtInquiryResponse;
import com.mparsict.vosolmaliat.application.service.FakeSanimDebtInquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fake-sanim/debt-inquiries")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasAuthority('AUDIT_VIEW')")
public class FakeSanimDebtInquiryController {

    private final FakeSanimDebtInquiryService service;

    @GetMapping
    public List<FakeSanimDebtInquiryResponse> list() {
        return service.list();
    }

    @PostMapping("/{id}/respond")
    @PreAuthorize("hasRole('ADMIN')")
    public FakeSanimDebtInquiryResponse respond(@PathVariable Long id) {
        return service.respond(id);
    }
}
