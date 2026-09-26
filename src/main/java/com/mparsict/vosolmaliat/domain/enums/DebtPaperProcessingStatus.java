package com.mparsict.vosolmaliat.domain.enums;

public enum DebtPaperProcessingStatus {
    RECEIVED,
    WAITING_IDENTITY,
    REJECTED_IDENTITY,
    NEEDS_REVIEW,
    WAITING_ORGANIZATION,
    PAPER_CREATED,
    SUCCESS,
    WORKFLOW_START_FAILED
}
