CREATE TABLE career_matches (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    resume_document_id BIGINT NOT NULL,
    job_description_document_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    report_json TEXT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_career_matches_owner
        FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_career_matches_resume
        FOREIGN KEY (resume_document_id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT fk_career_matches_job
        FOREIGN KEY (job_description_document_id) REFERENCES documents(id) ON DELETE CASCADE
);

CREATE INDEX idx_career_matches_owner ON career_matches(owner_id, created_at DESC);
