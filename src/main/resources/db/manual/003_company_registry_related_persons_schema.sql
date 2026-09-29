-- Normalize reusable legal-person relationships imported from the Companies
-- Registration System. This SQLite migration replaces role/post_name with
-- post_id and removes the obsolete execution_paper_id column.
--
-- The source related_persons table is expected to contain the columns from the
-- previous model, including owner_legal_person_id, role and source metadata.

PRAGMA foreign_keys = OFF;
BEGIN TRANSACTION;

CREATE TABLE IF NOT EXISTS related_person_post (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(255),
    is_deleted BOOLEAN
);

INSERT OR IGNORE INTO related_person_post (code, is_deleted) VALUES
    ('CEO', 0),
    ('BOARD_CHAIR', 0),
    ('BOARD_VICE_CHAIR', 0),
    ('BOARD_MEMBER', 0),
    ('ALTERNATE_BOARD_MEMBER', 0),
    ('MANAGING_PARTNER', 0),
    ('PARTNER', 0),
    ('SHAREHOLDER', 0),
    ('LEGAL_REPRESENTATIVE', 0),
    ('LEGAL_ENTITY_REPRESENTATIVE', 0),
    ('LIQUIDATOR', 0),
    ('PRIMARY_INSPECTOR', 0),
    ('ALTERNATE_INSPECTOR', 0),
    ('PARENT_COMPANY', 0),
    ('SUBSIDIARY', 0),
    ('AFFILIATE', 0);

CREATE TABLE related_persons_normalized (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    taxpayer_id INTEGER NOT NULL,
    owner_legal_person_id INTEGER NOT NULL,
    post_id INTEGER NOT NULL,
    start_date VARCHAR(255),
    end_date VARCHAR(255),
    position_expiry_date VARCHAR(255),
    is_board_member BOOLEAN NOT NULL,
    is_partner BOOLEAN NOT NULL,
    has_signatory_authority BOOLEAN NOT NULL,
    management_declaration INTEGER,
    source_system VARCHAR(255),
    source_record_id VARCHAR(255),
    last_synced_at VARCHAR(255),
    natural_person_id INTEGER,
    legal_person_id INTEGER,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(255),
    is_deleted BOOLEAN,
    CONSTRAINT fk_related_person_owner
        FOREIGN KEY (owner_legal_person_id) REFERENCES legal_person(id),
    CONSTRAINT fk_related_person_taxpayer
        FOREIGN KEY (taxpayer_id) REFERENCES taxpayer(id),
    CONSTRAINT fk_related_person_post
        FOREIGN KEY (post_id) REFERENCES related_person_post(id),
    CONSTRAINT fk_related_person_natural_target
        FOREIGN KEY (natural_person_id) REFERENCES natural_person(id),
    CONSTRAINT fk_related_person_legal_target
        FOREIGN KEY (legal_person_id) REFERENCES legal_person(id),
    CONSTRAINT ck_related_person_exactly_one_target CHECK (
        (natural_person_id IS NOT NULL AND legal_person_id IS NULL)
        OR
        (natural_person_id IS NULL AND legal_person_id IS NOT NULL)
    )
);

-- A missing/unknown role resolves to NULL and deliberately fails the NOT NULL
-- constraint, rolling back instead of silently losing or misclassifying data.
INSERT INTO related_persons_normalized (
    id,
    taxpayer_id,
    owner_legal_person_id,
    post_id,
    start_date,
    end_date,
    position_expiry_date,
    is_board_member,
    is_partner,
    has_signatory_authority,
    management_declaration,
    source_system,
    source_record_id,
    last_synced_at,
    natural_person_id,
    legal_person_id,
    created_at,
    updated_at,
    created_by,
    updated_by,
    deleted_at,
    deleted_by,
    is_deleted
)
SELECT
    relationship.id,
    (SELECT taxpayer.id FROM taxpayer WHERE taxpayer.legal_person_id = relationship.owner_legal_person_id LIMIT 1),
    relationship.owner_legal_person_id,
    (SELECT post.id FROM related_person_post post WHERE post.code = relationship.role),
    relationship.start_date,
    relationship.end_date,
    relationship.position_expiry_date,
    CASE WHEN relationship.is_non_board_member = 0 THEN 1 ELSE 0 END,
    CASE WHEN relationship.is_non_partner = 0 THEN 1 ELSE 0 END,
    relationship.has_signatory_authority,
    relationship.management_declaration,
    relationship.source_system,
    relationship.source_record_id,
    relationship.last_synced_at,
    relationship.natural_person_id,
    relationship.legal_person_id,
    relationship.created_at,
    relationship.updated_at,
    relationship.created_by,
    relationship.updated_by,
    relationship.deleted_at,
    relationship.deleted_by,
    relationship.is_deleted
FROM related_persons relationship;

DROP TABLE related_persons;
ALTER TABLE related_persons_normalized RENAME TO related_persons;

CREATE INDEX idx_related_person_owner
    ON related_persons(owner_legal_person_id);

CREATE INDEX idx_related_person_taxpayer
    ON related_persons(taxpayer_id);

CREATE INDEX idx_related_person_post
    ON related_persons(post_id);

CREATE UNIQUE INDEX uk_related_person_external_record
    ON related_persons(owner_legal_person_id, source_system, source_record_id);

COMMIT;
PRAGMA foreign_keys = ON;
