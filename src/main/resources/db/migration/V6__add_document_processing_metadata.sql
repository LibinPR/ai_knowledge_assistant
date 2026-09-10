ALTER TABLE documents
    ADD COLUMN document_type VARCHAR(32) NOT NULL DEFAULT 'SUPPORTING',
    ADD COLUMN processing_status VARCHAR(32) NOT NULL DEFAULT 'QUEUED',
    ADD COLUMN processing_error TEXT,
    ADD COLUMN source_content BYTEA,
    ADD COLUMN extracted_text TEXT,
    ADD COLUMN extracted_characters INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN page_count INTEGER,
    ADD COLUMN chunk_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN processing_started_at TIMESTAMP,
    ADD COLUMN processing_completed_at TIMESTAMP;

ALTER TABLE document_chunks
    ADD COLUMN chunk_index INTEGER,
    ADD COLUMN page_number INTEGER,
    ADD COLUMN section_title VARCHAR(255);

UPDATE documents d
SET processing_status = CASE
    WHEN EXISTS (SELECT 1 FROM document_chunks dc WHERE dc.document_id = d.id) THEN 'READY'
    ELSE 'FAILED'
END,
processing_error = CASE
    WHEN EXISTS (SELECT 1 FROM document_chunks dc WHERE dc.document_id = d.id) THEN NULL
    ELSE 'Document was created before processing status was available. Re-upload to process it.'
END;

CREATE INDEX idx_documents_owner_status ON documents(uploaded_by, processing_status);
