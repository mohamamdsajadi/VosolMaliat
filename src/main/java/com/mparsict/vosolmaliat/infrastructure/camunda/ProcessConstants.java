package com.mparsict.vosolmaliat.infrastructure.camunda;

public final class ProcessConstants {

    public static final String PROCESS_ID = "execution-paper-issuance";
    public static final String JOB_CHECK_ISSUANCE = "check-issuance-possibility";
    public static final String JOB_ISSUE_PAPER = "issue-execution-paper";
    public static final String JOB_SEND_REJECTION = "send-rejection-response";

    public static final String VAR_ISSUANCE_REQUEST_ID = "issuanceRequestId";
    public static final String VAR_DEBT_PAPER_PAYLOAD = "debtPaperPayload";
    public static final String VAR_CAN_ISSUE = "canIssue";
    public static final String VAR_REJECTION_CODE = "rejectionReasonCode";
    public static final String VAR_REJECTION_DESC = "rejectionReasonDesc";
    public static final String VAR_RESTRICTION_REMOVAL_DATE = "restrictionRemovalDate";
    public static final String VAR_EXECUTION_PAPER_CODE = "executionPaperCode";
    public static final String VAR_SUCCESS = "success";

    private ProcessConstants() {}
}
