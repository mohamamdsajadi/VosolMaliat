package com.mparsict.vosolmaliat.presentation.api;

import com.mparsict.vosolmaliat.application.dto.integration.CompanyRegistryRelatedPersonData;
import com.mparsict.vosolmaliat.application.service.FakeCompanyRegistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fake-company-registry")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('REPORT_VIEW','EXECUTION_PAPER_DETAIL_VIEW')")
public class FakeCompanyRegistryController {

    private final FakeCompanyRegistryService service;

    @GetMapping("/legal-persons/{nationalId}/related-persons")
    public List<CompanyRegistryRelatedPersonData> getRelatedPersons(@PathVariable String nationalId) {
        return service.fetchRelatedPersons(nationalId);
    }
}
