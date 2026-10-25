CREATE TABLE workspaces (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_workspaces_slug UNIQUE (slug),
    CONSTRAINT fk_workspaces_created_by
        FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE TABLE workspace_memberships (
    workspace_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (workspace_id, user_id),
    CONSTRAINT chk_workspace_memberships_role
        CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'VIEWER')),
    CONSTRAINT fk_workspace_memberships_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE CASCADE,
    CONSTRAINT fk_workspace_memberships_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_workspace_memberships_user_id
    ON workspace_memberships(user_id);

INSERT INTO workspaces (name, slug, created_by)
SELECT 'Personal', 'personal-' || id, id
FROM users;

INSERT INTO workspace_memberships (workspace_id, user_id, role)
SELECT w.id, w.created_by, 'OWNER'
FROM workspaces w;

ALTER TABLE documents
    ADD COLUMN workspace_id BIGINT;

UPDATE documents d
SET workspace_id = w.id
FROM workspaces w
WHERE w.slug = 'personal-' || d.uploaded_by;

ALTER TABLE documents
    ALTER COLUMN workspace_id SET NOT NULL,
    ADD CONSTRAINT fk_documents_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces(id) ON DELETE RESTRICT;

CREATE INDEX idx_documents_workspace_status
    ON documents(workspace_id, processing_status);
