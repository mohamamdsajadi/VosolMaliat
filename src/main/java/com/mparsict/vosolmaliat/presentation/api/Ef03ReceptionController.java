package com.mparsict.vosolmaliat.presentation.api;

import com.mparsict.vosolmaliat.application.dto.request.DebtPaperPushRequest;
import com.mparsict.vosolmaliat.application.dto.response.ApiGatewayResponse;
import com.mparsict.vosolmaliat.application.dto.response.DebtPaperPushData;
import com.mparsict.vosolmaliat.application.service.DebtPaperPushService;
import com.mparsict.vosolmaliat.common.web.DebtPaperTraceFilter;
import com.mparsict.vosolmaliat.application.service.Ef03ReceptionService;
import com.mparsict.vosolmaliat.application.service.Ef03SampleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/ef03")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USER_PROFILE_ASSIGN_ORGANIZATION')")
public class Ef03ReceptionController {
    private final Ef03ReceptionService reception;
    private final Ef03SampleService samples;
    private final DebtPaperPushService debtPaperPushService;

    @GetMapping("/samples")
    public java.util.List<Ef03SampleService.Sample> samples() {
        return samples.samples();
    }

    @PostMapping("/push")
    public ResponseEntity<ApiGatewayResponse<DebtPaperPushData>> push(
            @Valid @RequestBody DebtPaperPushRequest request,
            HttpServletRequest httpRequest) {
        String traceId = DebtPaperTraceFilter.traceId(httpRequest);
        Long executionPaperId = debtPaperPushService.pushImmediately(request, traceId);
        String message = "برگه بدهی دریافت و برگه اجرا با موفقیت ایجاد شد.";
        return ResponseEntity.ok(
                ApiGatewayResponse.success(new DebtPaperPushData(executionPaperId), traceId, message));
    }

    @PostMapping("/receive")
    public ResponseEntity<Ef03ReceptionService.Result> receive(@Valid @RequestBody Receipt input) {
        var result = reception.receive(input.xml(), input.referenceId(), input.followCode(),
                input.taxOfficeCode(), input.taxSource());
        return ResponseEntity.status(result.executionPaperId() == null ? 202 : 200).body(result);
    }

    public record Receipt(
            @NotBlank String xml,
            @NotBlank String referenceId,
            @NotBlank String followCode,
            String taxOfficeCode,
            @NotBlank String taxSource
    ) {}
}
