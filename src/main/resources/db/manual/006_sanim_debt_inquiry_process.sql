-- Persistence for the GetLastDebtStatus BPMN process and its fake SANIM integration.

CREATE TABLE IF NOT EXISTS fake_sanim_debt_inquiry_request (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    request_id VARCHAR(255) NOT NULL UNIQUE,
    execution_paper_id INTEGER NOT NULL,
    taxpayer_type VARCHAR(32) NOT NULL,
    taxpayer_identifier VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    sent_at TIMESTAMP NOT NULL,
    responded_at TIMESTAMP,
    response_payload TEXT,
    error_message TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(255),
    is_deleted BOOLEAN
);

CREATE INDEX IF NOT EXISTS idx_fake_sanim_debt_execution_paper
    ON fake_sanim_debt_inquiry_request(execution_paper_id);

CREATE TABLE IF NOT EXISTS debt_inquiry_result (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    request_id VARCHAR(255) NOT NULL UNIQUE,
    execution_paper_id INTEGER NOT NULL,
    response_json TEXT NOT NULL,
    hash VARCHAR(64) NOT NULL,
    received_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(255),
    is_deleted BOOLEAN,
    FOREIGN KEY (execution_paper_id) REFERENCES execution_paper(id)
);

CREATE INDEX IF NOT EXISTS idx_debt_inquiry_execution_paper
    ON debt_inquiry_result(execution_paper_id);

ALTER TABLE debt_information ADD COLUMN sanim_total_debt_rials BIGINT;
ALTER TABLE debt_information ADD COLUMN sanim_total_penalty_rials BIGINT;
ALTER TABLE debt_information ADD COLUMN sanim_total_payment_rials BIGINT;
ALTER TABLE debt_information ADD COLUMN sanim_total_credit_rials BIGINT;
ALTER TABLE debt_information ADD COLUMN sanim_outstanding_debt_rials BIGINT;
ALTER TABLE debt_information ADD COLUMN sanim_last_activity_at TIMESTAMP;
ALTER TABLE debt_information ADD COLUMN last_debt_inquiry_at TIMESTAMP;
