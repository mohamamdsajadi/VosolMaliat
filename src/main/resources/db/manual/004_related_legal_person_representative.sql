-- Add the mandatory natural-person representative of a related legal person.
-- Apply after 003_company_registry_related_persons_schema.sql.

PRAGMA foreign_keys = ON;
BEGIN TRANSACTION;

ALTER TABLE related_persons
    ADD COLUMN legal_representative_natural_person_id INTEGER
        REFERENCES natural_person(id);

-- Existing data in this project comes from the fake registry, where the CEO is
-- the representative of the related legal person. Prefer that record and fall
-- back to the first natural relationship for the same owner only for legacy
-- fake data created before this column existed.
UPDATE related_persons
SET legal_representative_natural_person_id = (
    SELECT candidate.natural_person_id
    FROM related_persons candidate
    JOIN related_person_post candidate_post ON candidate_post.id = candidate.post_id
    WHERE candidate.owner_legal_person_id = related_persons.owner_legal_person_id
      AND candidate.natural_person_id IS NOT NULL
    ORDER BY
        CASE WHEN candidate_post.code = 'CEO' THEN 0 ELSE 1 END,
        candidate.id
    LIMIT 1
)
WHERE legal_person_id IS NOT NULL
  AND legal_representative_natural_person_id IS NULL;

-- Abort instead of accepting a legal target without a representative.
CREATE TEMP TABLE related_legal_representative_validation (
    invalid_count INTEGER NOT NULL CHECK (invalid_count = 0)
);

INSERT INTO related_legal_representative_validation (invalid_count)
SELECT COUNT(*)
FROM related_persons
WHERE (legal_person_id IS NOT NULL AND legal_representative_natural_person_id IS NULL)
   OR (legal_person_id IS NULL AND legal_representative_natural_person_id IS NOT NULL);

DROP TABLE related_legal_representative_validation;

CREATE INDEX idx_related_person_legal_representative
    ON related_persons(legal_representative_natural_person_id);

CREATE TRIGGER ck_related_person_legal_representative_insert
BEFORE INSERT ON related_persons
WHEN (NEW.legal_person_id IS NOT NULL AND NEW.legal_representative_natural_person_id IS NULL)
  OR (NEW.legal_person_id IS NULL AND NEW.legal_representative_natural_person_id IS NOT NULL)
BEGIN
    SELECT RAISE(ABORT, 'related legal person must have a natural-person representative');
END;

CREATE TRIGGER ck_related_person_legal_representative_update
BEFORE UPDATE OF legal_person_id, legal_representative_natural_person_id ON related_persons
WHEN (NEW.legal_person_id IS NOT NULL AND NEW.legal_representative_natural_person_id IS NULL)
  OR (NEW.legal_person_id IS NULL AND NEW.legal_representative_natural_person_id IS NOT NULL)
BEGIN
    SELECT RAISE(ABORT, 'related legal person must have a natural-person representative');
END;

COMMIT;
