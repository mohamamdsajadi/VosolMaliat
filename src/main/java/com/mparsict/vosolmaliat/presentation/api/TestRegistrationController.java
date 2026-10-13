package com.mparsict.vosolmaliat.presentation.api;

import com.mparsict.vosolmaliat.application.dto.request.TestRegistrationRequest;
import com.mparsict.vosolmaliat.application.dto.response.TestRegistrationResponse;
import com.mparsict.vosolmaliat.application.service.TestRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test/registrations")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.test.registration.enabled", havingValue = "true")
public class TestRegistrationController {

    private final TestRegistrationService registrationService;

    @PostMapping
    public TestRegistrationResponse register(@RequestBody @Valid TestRegistrationRequest request) {
        return registrationService.register(request);
    }
}
