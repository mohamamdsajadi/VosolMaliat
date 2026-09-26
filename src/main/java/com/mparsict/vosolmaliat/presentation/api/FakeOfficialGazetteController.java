package com.mparsict.vosolmaliat.presentation.api;

import com.mparsict.vosolmaliat.application.dto.integration.CompanyRegistryRelatedPersonData;
import com.mparsict.vosolmaliat.application.dto.response.FakeOfficialGazetteResponse;
import com.mparsict.vosolmaliat.application.service.FakeOfficialGazetteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fake-official-gazette")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasAuthority('AUDIT_VIEW')")
public class FakeOfficialGazetteController {

    private final FakeOfficialGazetteService service;

    @GetMapping("/requests")
    public List<FakeOfficialGazetteResponse> list() {
        return service.list();
    }

    @GetMapping("/requests/{id}/preview")
    public List<CompanyRegistryRelatedPersonData> preview(@PathVariable Long id) {
        return service.preview(id);
    }

    @PostMapping("/requests/{id}/respond")
    public FakeOfficialGazetteResponse respond(
            @PathVariable Long id,
            @RequestBody(required = false) List<CompanyRegistryRelatedPersonData> relatedPersons) {
        return service.respond(id, relatedPersons);
    }
}
