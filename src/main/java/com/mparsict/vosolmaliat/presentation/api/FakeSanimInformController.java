package com.mparsict.vosolmaliat.presentation.api;

import com.mparsict.vosolmaliat.application.dto.response.FakeSanimInformResponse;
import com.mparsict.vosolmaliat.application.service.FakeSanimInformService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fake-sanim/inform-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasAuthority('AUDIT_VIEW')")
public class FakeSanimInformController {

    private final FakeSanimInformService service;

    @GetMapping
    public List<FakeSanimInformResponse> list() {
        return service.list();
    }

    @PostMapping("/{id}/confirm")
    public FakeSanimInformResponse confirm(@PathVariable Long id) {
        return service.confirm(id);
    }
}
