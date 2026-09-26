-- Fake external systems used by the Tabsare3-186 test process.

CREATE TABLE IF NOT EXISTS fake_official_gazette_request (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    request_id VARCHAR(255) NOT NULL UNIQUE,
    execution_paper_id INTEGER NOT NULL,
    legal_person_national_id VARCHAR(255) NOT NULL,
    debt_paper_date VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    sent_at VARCHAR(255),
    responded_at VARCHAR(255),
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

CREATE INDEX IF NOT EXISTS idx_fake_official_gazette_execution_paper
    ON fake_official_gazette_request(execution_paper_id);

CREATE TABLE IF NOT EXISTS fake_restriction_request (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    official_gazette_request_id VARCHAR(255) NOT NULL UNIQUE,
    execution_paper_id INTEGER NOT NULL,
    status VARCHAR(255) NOT NULL,
    related_person_count INTEGER NOT NULL,
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
    is_deleted BOOLEAN
);

CREATE INDEX IF NOT EXISTS idx_fake_restriction_execution_paper
    ON fake_restriction_request(execution_paper_id);

CREATE INDEX IF NOT EXISTS idx_fake_restriction_official_request
    ON fake_restriction_request(official_gazette_request_id);
