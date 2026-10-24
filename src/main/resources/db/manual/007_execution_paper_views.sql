-- Personal details-page viewing metadata. Development uses Hibernate ddl-auto=update.
CREATE TABLE IF NOT EXISTS execution_paper_view (
    user_key VARCHAR(255) NOT NULL,
    execution_paper_id INTEGER NOT NULL REFERENCES execution_paper(id),
    last_viewed_at_millis BIGINT NOT NULL,
    PRIMARY KEY (user_key, execution_paper_id)
);
