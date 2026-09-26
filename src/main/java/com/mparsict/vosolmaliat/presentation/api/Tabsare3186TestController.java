package com.mparsict.vosolmaliat.presentation.api;

import com.mparsict.vosolmaliat.application.dto.request.Tabsare3186TestStartRequest;
import com.mparsict.vosolmaliat.application.dto.response.Tabsare3186TestStartResponse;
import com.mparsict.vosolmaliat.infrastructure.camunda.service.CamundaProcessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/test/tabsare3-186")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasAuthority('AUDIT_VIEW')")
public class Tabsare3186TestController {

    private final CamundaProcessService camundaProcessService;

    @PostMapping("/start")
    public Tabsare3186TestStartResponse start(@RequestBody @Valid Tabsare3186TestStartRequest request) {
        log.info("Starting Tabsare3-186 test process for executionPaperId={}", request.executionPaperId());
        return camundaProcessService.startTabsare3186TestProcess(request);
    }
}
