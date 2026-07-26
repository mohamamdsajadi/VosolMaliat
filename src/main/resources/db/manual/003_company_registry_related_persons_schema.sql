-- Reusable legal-person relationships imported from the Companies Registration System.
-- Development uses Hibernate ddl-auto=update. Apply this migration explicitly in production.

ALTER TABLE related_persons ADD COLUMN owner_legal_person_id INTEGER;
ALTER TABLE related_persons ADD COLUMN role VARCHAR(255);
ALTER TABLE related_persons ADD COLUMN source_system VARCHAR(255);
ALTER TABLE related_persons ADD COLUMN source_record_id VARCHAR(255);
ALTER TABLE related_persons ADD COLUMN last_synced_at VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_related_person_owner
    ON related_persons(owner_legal_person_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_related_person_external_record
    ON related_persons(owner_legal_person_id, source_system, source_record_id);
