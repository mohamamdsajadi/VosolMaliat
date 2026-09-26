package com.mparsict.vosolmaliat.presentation.api;

import com.mparsict.vosolmaliat.application.dto.integration.CompanyRegistryRelatedPersonData;
import com.mparsict.vosolmaliat.application.dto.response.FakeRestrictionResponse;
import com.mparsict.vosolmaliat.application.service.FakeRestrictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fake-restrictions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasAuthority('AUDIT_VIEW')")
public class FakeRestrictionController {

    private final FakeRestrictionService service;

    @GetMapping("/requests")
    public List<FakeRestrictionResponse> list() {
        return service.list();
    }

    @PostMapping("/requests/{id}/confirm")
    public FakeRestrictionResponse confirm(
            @PathVariable Long id,
            @RequestBody(required = false) List<CompanyRegistryRelatedPersonData> relatedPersons) {
        return service.confirm(id, relatedPersons);
    }
}
