package com.mparsict.vosolmaliat.common.exception;

import com.mparsict.vosolmaliat.application.dto.response.ApiGatewayResponse;
import com.mparsict.vosolmaliat.application.dto.response.DebtPaperPushData;
import com.mparsict.vosolmaliat.common.web.DebtPaperTraceFilter;
import com.mparsict.vosolmaliat.presentation.api.DebtPaperPushController;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = DebtPaperPushController.class)
public class DebtPaperPushExceptionHandler {

    @ExceptionHandler(DebtPaperPushException.class)
    public ResponseEntity<ApiGatewayResponse<DebtPaperPushData>> handleDebtPaperFailure(
            DebtPaperPushException exception,
            HttpServletRequest request) {
        String traceId = DebtPaperTraceFilter.traceId(request);
        log.warn("Debt Paper request failed: traceID={}, statusCode={}, reason={}",
                traceId, exception.getApiStatusCode(), exception.getMessage());
        return ResponseEntity.status(exception.getHttpStatus()).body(ApiGatewayResponse.failure(
                new DebtPaperPushData(exception.getExecutionPaperId()),
                traceId,
                true,
                exception.getApiStatusCode(),
                exception.getMessage()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiGatewayResponse<DebtPaperPushData>> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> externalFieldName(error.getField()) + ": " + error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));
        return invalidRequest(request, message.isBlank() ? "Debt Paper data is invalid." : message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiGatewayResponse<DebtPaperPushData>> handleUnreadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return invalidRequest(request, "Debt Paper JSON is invalid or contains an unsupported value.");
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiGatewayResponse<DebtPaperPushData>> handleBusiness(BusinessException exception, HttpServletRequest request) {
        if ("PROCESS_START_FAILED".equals(exception.getErrorCode())) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiGatewayResponse.failure(
                    DebtPaperPushData.empty(), DebtPaperTraceFilter.traceId(request), true, -99,
                    "Camunda result unavailable; the stored request may still be processing."));
        }
        return ResponseEntity.unprocessableEntity().body(ApiGatewayResponse.failure(DebtPaperPushData.empty(),
                DebtPaperTraceFilter.traceId(request),true,-7,exception.getErrorCode()+": "+exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiGatewayResponse<DebtPaperPushData>> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {
        String traceId = DebtPaperTraceFilter.traceId(request);
        log.error("Unexpected Debt Paper processing failure: traceID={}", traceId, exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiGatewayResponse.failure(
                DebtPaperPushData.empty(),
                traceId,
                true,
                -99,
                "An unexpected internal processing failure occurred."
        ));
    }

    private ResponseEntity<ApiGatewayResponse<DebtPaperPushData>> invalidRequest(
            HttpServletRequest request,
            String message) {
        String traceId = DebtPaperTraceFilter.traceId(request);
        log.warn("Invalid Debt Paper request: traceID={}, reason={}", traceId, message);
        return ResponseEntity.badRequest().body(ApiGatewayResponse.failure(
                DebtPaperPushData.empty(), traceId, true, -7, message
        ));
    }

    private String externalFieldName(String field) {
        return switch (field) {
            case "referenceId" -> "referenceID";
            case "taxOfficeId" -> "taxOfficeID";
            default -> field;
        };
    }
}
