package com.mparsict.vosolmaliat.infrastructure.camunda;

public final class ProcessConstants {

    public static final String PROCESS_ID = "Process_0rtbdc5";
    public static final String JOB_CHECK_ISSUANCE = "check-issuance-possibility";
    public static final String JOB_ISSUE_PAPER = "issue-execution-paper";
    public static final String JOB_SEND_REJECTION = "inform_sanim_rejection";
    public static final String JOB_SEND_EXECUTION_PAPER_ID_TO_SANIM = "send_execution_paper_id_to_sanim";
    public static final String JOB_CHECK_INFORM = "check-inform-possibility";
    public static final String JOB_SEND_TO_ELECTRONIC_NOTIFICATION = "sendExecutionPaperToEB";
    public static final String JOB_UPDATE_INFORM_STATUS = "update-inform-status";
    public static final String JOB_SET_RE_INFORM_DATE = "set-re-inform-date";
    public static final String JOB_SET_INQUIRE = "set-inquire";
    public static final String JOB_SEND_INFORM_DATA_TO_SANIM = "send-inform-data-to-sanim";

    public static final String MESSAGE_EXECUTION_PAPER_INFORMED = "ExecutionPaperInformed";

    public static final String VAR_ISSUANCE_REQUEST_ID = "issuanceRequestId";
    public static final String VAR_DEBT_PAPER_PAYLOAD = "debtPaperPayload";
    public static final String VAR_CAN_ISSUE = "canIssue";
    public static final String VAR_REJECTION_CODE = "rejectionReasonCode";
    public static final String VAR_REJECTION_DESC = "rejectionReasonDesc";
    public static final String VAR_RESTRICTION_REMOVAL_DATE = "restrictionRemovalDate";
    public static final String VAR_EXECUTION_PAPER_ID = "executionPaperId";
    public static final String VAR_EXECUTION_PAPER_CODE = "executionPaperCode";
    public static final String VAR_GENERAL_DIRECTORATE_ID = "generalDirectorateId";
    public static final String VAR_TAX_AFFAIRS_OFFICE_ID = "taxAffairsOfficeId";
    public static final String VAR_TAX_UNIT_ID = "taxUnitId";
    public static final String VAR_GENERAL_OFFICE_HEAD_CANDIDATE_GROUPS = "generalOfficeHeadCandidateGroups";
    public static final String VAR_TAX_OFFICE_HEAD_CANDIDATE_GROUPS = "taxOfficeHeadCandidateGroups";
    public static final String VAR_TAX_UNIT_HEAD_CANDIDATE_GROUPS = "taxUnitHeadCandidateGroups";
    public static final String VAR_TAX_EXPERT_CANDIDATE_GROUPS = "taxExpertCandidateGroups";
    public static final String VAR_SUCCESS = "success";
    public static final String VAR_CAN_INFORM = "canInform";
    public static final String VAR_INFORM_DATE = "InformDate";
    public static final String VAR_RE_INFORM_DATE = "reInformDate";
    public static final String VAR_NEXT_DEBT_INQUIRY_DATE = "nextDebtInquiryDate";

    private ProcessConstants() {}
}
