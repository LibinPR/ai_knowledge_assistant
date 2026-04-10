CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE documents (
                           id          BIGSERIAL PRIMARY KEY,
                           filename    VARCHAR(255) NOT NULL,
                           uploaded_by BIGINT NOT NULL,
                           uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT fk_documents_user
                               FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_documents_uploaded_by ON documents(uploaded_by);

CREATE TABLE document_chunks (
                                 id          BIGSERIAL PRIMARY KEY,
                                 document_id BIGINT NOT NULL,
                                 content     TEXT,
                                 embedding   vector(384),

                                 CONSTRAINT fk_chunks_document
                                     FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

CREATE INDEX idx_document_chunks_document_id ON document_chunks(document_id);