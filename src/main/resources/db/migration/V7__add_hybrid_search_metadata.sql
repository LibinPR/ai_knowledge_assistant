ALTER TABLE document_chunks
    ADD COLUMN search_vector tsvector
        GENERATED ALWAYS AS (to_tsvector('simple', coalesce(content, ''))) STORED;

CREATE INDEX idx_document_chunks_search_vector
    ON document_chunks USING GIN (search_vector);

CREATE INDEX idx_document_chunks_ready_document
    ON document_chunks(document_id);
