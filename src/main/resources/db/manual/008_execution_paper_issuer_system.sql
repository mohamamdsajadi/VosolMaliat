-- Run once on an existing execution_paper table when applying manual migrations.
-- Application startup performs the same migration idempotently for SQLite.
ALTER TABLE execution_paper ADD COLUMN issuer_system VARCHAR(16)
    NOT NULL DEFAULT 'SANIM' CHECK (issuer_system IN ('SANIM', 'TABRIZ'));
