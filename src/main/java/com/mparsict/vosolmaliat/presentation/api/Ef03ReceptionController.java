package com.mparsict.vosolmaliat.presentation.api;

import com.mparsict.vosolmaliat.application.service.Ef03SampleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/ef03")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USER_PROFILE_ASSIGN_ORGANIZATION')")
public class Ef03ReceptionController {
    private final Ef03SampleService samples;

    @GetMapping("/samples")
    public java.util.List<Ef03SampleService.Sample> samples() {
        return samples.samples();
    }
}
