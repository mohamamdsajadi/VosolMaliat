package com.mparsict.vosolmaliat.presentation.api;

import com.mparsict.vosolmaliat.application.dto.request.DebtPaperPushRequest;
import com.mparsict.vosolmaliat.application.dto.response.ApiGatewayResponse;
import com.mparsict.vosolmaliat.application.dto.response.DebtPaperPushData;
import com.mparsict.vosolmaliat.application.dto.response.IssuanceResponse;
import com.mparsict.vosolmaliat.application.service.DebtPaperPushService;
import com.mparsict.vosolmaliat.common.web.DebtPaperTraceFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/debt-papers")
@RequiredArgsConstructor
public class DebtPaperPushController {

    private final DebtPaperPushService debtPaperPushService;

    @PostMapping(value = "/push", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiGatewayResponse<DebtPaperPushData>> push(
            @RequestBody @Valid DebtPaperPushRequest request,
            HttpServletRequest httpRequest) {
        String traceId = DebtPaperTraceFilter.traceId(httpRequest);
        IssuanceResponse result = debtPaperPushService.push(request, traceId);
        DebtPaperPushData data = DebtPaperPushData.from(result);
        if (!result.success()) {
            String message = result.rejectionReasonDescription() == null
                    ? result.message() : result.rejectionReasonDescription();
            return ResponseEntity.unprocessableEntity().body(ApiGatewayResponse.failure(
                    data, traceId, true, -7, message));
        }
        return ResponseEntity.ok(ApiGatewayResponse.success(data, traceId, result.message()));
    }
}
