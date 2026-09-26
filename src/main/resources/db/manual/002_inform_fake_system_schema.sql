-- Manual schema for the inform subprocess and fake external-system panels.
-- Development currently uses Hibernate ddl-auto=update, but production should apply an explicit migration.

CREATE TABLE IF NOT EXISTS execution_paper_inform_rejection_rule (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    rule_json TEXT,
    start_date VARCHAR(255),
    end_date VARCHAR(255),
    document_no VARCHAR(255),
    file_path VARCHAR(255),
    description TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(255),
    is_deleted BOOLEAN
);

CREATE TABLE IF NOT EXISTS fake_electronic_notification_request (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    execution_paper_id INTEGER,
    external_tracking_code VARCHAR(255),
    status VARCHAR(255),
    payload TEXT,
    sent_at VARCHAR(255),
    inform_date VARCHAR(255),
    confirmed_at VARCHAR(255),
    callback_delivered_at VARCHAR(255),
    error_message TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(255),
    is_deleted BOOLEAN,
    CONSTRAINT fk_fake_eb_execution_paper FOREIGN KEY (execution_paper_id) REFERENCES execution_paper(id)
);

CREATE TABLE IF NOT EXISTS fake_sanim_inform_request (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    execution_paper_id INTEGER,
    status VARCHAR(255),
    inform_date VARCHAR(255),
    payload TEXT,
    sent_at VARCHAR(255),
    confirmed_at VARCHAR(255),
    error_message TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(255),
    is_deleted BOOLEAN,
    CONSTRAINT fk_fake_sanim_execution_paper FOREIGN KEY (execution_paper_id) REFERENCES execution_paper(id)
);

ALTER TABLE fake_sanim_inform_request ADD COLUMN message_type VARCHAR(255);
ALTER TABLE fake_sanim_inform_request ADD COLUMN debt_paper_id VARCHAR(255);
ALTER TABLE fake_sanim_inform_request ADD COLUMN rejection_reason_code VARCHAR(255);
ALTER TABLE fake_sanim_inform_request ADD COLUMN rejection_reason_description TEXT;
ALTER TABLE fake_sanim_inform_request ADD COLUMN restriction_removal_date VARCHAR(255);

ALTER TABLE execution_paper_notification ADD COLUMN inform_status VARCHAR(255);
ALTER TABLE execution_paper_notification ADD COLUMN inform_rejection_reason TEXT;
ALTER TABLE execution_paper_notification ADD COLUMN inform_rejected_at VARCHAR(255);
ALTER TABLE execution_paper_notification ADD COLUMN re_inform_date VARCHAR(255);
ALTER TABLE execution_paper_notification ADD COLUMN next_debt_inquiry_date VARCHAR(255);
