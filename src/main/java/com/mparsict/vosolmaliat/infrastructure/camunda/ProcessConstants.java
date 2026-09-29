package com.mparsict.vosolmaliat.infrastructure.camunda;

public final class ProcessConstants {

    public static final String PROCESS_ID = "Process_0rtbdc5";
    public static final String TABSARE_3_186_PROCESS_ID = "Process_0eqvh8j";
    public static final String GET_LAST_DEBT_STATUS_PROCESS_ID = "Process_09srbi7";
    /**
     * Versioned because older application deployments used automatic completion for this job.
     * Keeping a distinct type prevents a stale worker from completing a new process without
     * publishing the required {@link #VAR_CAN_ISSUE} variable.
     */
    public static final String JOB_CHECK_ISSUANCE = "check-issuance-possibility-v2";
    /**
     * Versioned so an older application replica with framework auto-completion enabled cannot
     * advance a new process after issuance persistence fails.
     */
    public static final String JOB_ISSUE_PAPER = "issue-execution-paper-v2";
    public static final String JOB_SEND_REJECTION = "inform_sanim_rejection";
    public static final String JOB_SEND_EXECUTION_PAPER_ID_TO_SANIM = "send_execution_paper_id_to_sanim";
    public static final String JOB_CHECK_INFORM = "check-inform-possibility";
    public static final String JOB_SEND_TO_ELECTRONIC_NOTIFICATION = "sendExecutionPaperToEB";
    public static final String JOB_UPDATE_INFORM_STATUS = "update-inform-status";
    public static final String JOB_SET_RE_INFORM_DATE = "set-re-inform-date";
    public static final String JOB_SET_INQUIRE = "set-inquire";
    public static final String JOB_SEND_INFORM_DATA_TO_SANIM = "send-inform-data-to-sanim";
    public static final String JOB_SEND_OFFICIAL_GAZETTE_BOARD_MEMBERS_REQUEST =
            "send-official-gazette-board-members-request";
    public static final String JOB_FILTER_RELATED_PERSONS_BY_DEBT_PAPER_DATE =
            "filter-related-persons-by-debt-paper-date";
    public static final String JOB_SEND_RELATED_PERSONS_FOR_RESTRICTIONS =
            "send-related-persons-for-restrictions";
    public static final String JOB_STORE_RELATED_PERSONS = "store-related-persons";
    public static final String JOB_GET_DEBT_FROM_SANIM = "get-debt-from-sanim";
    public static final String JOB_UPDATE_TAXPAYER_PAYMENT_STATUS = "update-taxpayer-payment-status";

    public static final String MESSAGE_EXECUTION_PAPER_INFORMED = "ExecutionPaperInformed";
    public static final String MESSAGE_OFFICIAL_GAZETTE_RELATED_PERSONS_RECEIVED =
            "official_gazette_related_persons_received";
    public static final String MESSAGE_RESTRICTIONS_APPLIED = "restrictions_applied";
    public static final String MESSAGE_TAXPAYER_DEBT = "taxPayerDebt";

    public static final String VAR_ISSUANCE_REQUEST_ID = "issuanceRequestId";
    public static final String VAR_DEBT_PAPER_PAYLOAD = "debtPaperPayload";
    public static final String VAR_CAN_ISSUE = "canIssue";
    public static final String VAR_REJECTION_CODE = "rejectionReasonCode";
    public static final String VAR_REJECTION_DESC = "rejectionReasonDesc";
    public static final String VAR_RESTRICTION_REMOVAL_DATE = "restrictionRemovalDate";
    public static final String VAR_EXECUTION_PAPER_ID = "executionPaperId";
    public static final String VAR_LEGAL_PERSON_NATIONAL_ID = "legalPersonNationalId";
    public static final String VAR_DEBT_PAPER_DATE = "debtPaperDate";
    public static final String VAR_OFFICIAL_GAZETTE_REQUEST_ID = "officialGazetteRequestId";
    public static final String VAR_OFFICIAL_GAZETTE_RELATED_PERSONS = "officialGazetteRelatedPersons";
    public static final String VAR_FILTERED_RELATED_PERSONS = "filteredRelatedPersons";
    public static final String VAR_RESTRICTION_REQUEST_ID = "restrictionRequestId";
    public static final String VAR_RESTRICTIONS_APPLIED = "restrictionsApplied";
    public static final String VAR_EXECUTION_PAPER_CODE = "executionPaperCode";
    public static final String VAR_GENERAL_DIRECTORATE_ID = "generalDirectorateId";
    public static final String VAR_TAX_AFFAIRS_OFFICE_ID = "taxAffairsOfficeId";
    public static final String VAR_GENERAL_OFFICE_HEAD_CANDIDATE_GROUPS = "generalOfficeHeadCandidateGroups";
    public static final String VAR_TAX_OFFICE_HEAD_CANDIDATE_GROUPS = "taxOfficeHeadCandidateGroups";
    public static final String VAR_TAX_EXPERT_CANDIDATE_GROUPS = "taxExpertCandidateGroups";
    public static final String VAR_SUCCESS = "success";
    public static final String VAR_CAN_INFORM = "canInform";
    public static final String VAR_INFORM_DATE = "InformDate";
    public static final String VAR_RE_INFORM_DATE = "reInformDate";
    public static final String VAR_NEXT_DEBT_INQUIRY_DATE = "nextDebtInquiryDate";
    public static final String VAR_DEBT_INQUIRY_REQUEST_ID = "debtInquiryRequestId";
    public static final String VAR_SANIM_DEBT_STATUS = "sanimDebtStatus";
    public static final String VAR_DEBT_STATUS_UPDATED = "debtStatusUpdated";
    public static final String VAR_FINISH_EXECUTION_PAPER_CHECK = "finishExecutionPaperCheck";

    public static String restrictionRequestId(String officialGazetteRequestId) {
        return "restriction-" + officialGazetteRequestId;
    }

    private ProcessConstants() {}
}
