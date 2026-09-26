package com.mparsict.vosolmaliat.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class DebtPaperPushException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final int apiStatusCode;
    private final Long executionPaperId;

    public DebtPaperPushException(HttpStatus httpStatus, int apiStatusCode, String message) {
        this(httpStatus, apiStatusCode, message, 0L, null);
    }

    public DebtPaperPushException(
            HttpStatus httpStatus,
            int apiStatusCode,
            String message,
            Long executionPaperId,
            Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.apiStatusCode = apiStatusCode;
        this.executionPaperId = executionPaperId == null ? 0L : executionPaperId;
    }
}
