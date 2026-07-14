package com.mparsict.vosolmaliat.presentation.api;

import com.mparsict.vosolmaliat.application.dto.request.FakeElectronicNotificationCallbackRequest;
import com.mparsict.vosolmaliat.application.dto.response.FakeElectronicNotificationResponse;
import com.mparsict.vosolmaliat.application.service.FakeElectronicNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fake-electronic-notification")
@RequiredArgsConstructor
public class FakeElectronicNotificationController {

    private final FakeElectronicNotificationService service;

    @GetMapping("/requests")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('AUDIT_VIEW')")
    public List<FakeElectronicNotificationResponse> list() {
        return service.list();
    }

    @PostMapping("/requests/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('AUDIT_VIEW')")
    public FakeElectronicNotificationResponse confirm(
            @PathVariable Long id,
            @RequestBody(required = false) FakeElectronicNotificationCallbackRequest request) {
        String informDate = request == null ? null : request.InformDate();
        return service.confirm(id, informDate);
    }

    @PostMapping("/callback")
    public FakeElectronicNotificationResponse callback(@RequestBody @Valid FakeElectronicNotificationCallbackRequest request) {
        return service.confirmByExecutionPaper(request.executionPaperId(), request.InformDate());
    }
}
