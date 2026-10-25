# V1 Workspace and Membership Design

Status: V1.1 implemented. V1.2 workspace-scoped APIs and authorization remain pending.

## Problem

Documents are currently isolated by `uploaded_by`. That works for a personal RAG
application, but it cannot represent shared knowledge, team permissions, or a durable
tenant boundary for future agent runs and integrations.

## Decisions

### Workspace is the authorization boundary

Every knowledge resource will belong to one workspace. A document keeps its uploader
for attribution, but access is controlled through its workspace membership.

```text
User --< WorkspaceMembership >-- Workspace --< Document --< DocumentChunk
                                            \--< future Collection
                                            \--< future AgentRun
```

### Keep global and workspace roles separate

The existing `USER` and `ADMIN` roles remain global application roles:

- `USER`: normal authenticated account.
- `ADMIN`: system operator; current administrative behavior remains explicit and does
  not become a workspace role by accident.

Workspace membership uses a small, separate enum, not the global `roles` table:

- `OWNER`: manages the workspace and members.
- `ADMIN`: manages workspace knowledge and members, but is not the last-resort owner.
- `MEMBER`: uploads, queries, and uses normal workspace workflows.
- `VIEWER`: reads and queries approved workspace knowledge only.

This avoids mixing application administration with team-level permissions.

### Do not put active-workspace authorization in the JWT

Membership is checked against the database on workspace-scoped requests. JWTs continue
to establish the user identity only. This makes removed memberships effective without
waiting for token expiry and avoids embedding an unbounded membership list in tokens.

### Require workspace identity in the resource path

New workspace-aware routes will use an explicit path parameter, for example:

```text
POST /api/workspaces/{workspaceId}/documents
POST /api/workspaces/{workspaceId}/query
GET  /api/workspaces/{workspaceId}/documents
```

The backend verifies membership before retrieving or mutating a resource. The frontend
may remember a selected workspace, but it is never the authorization authority.

## Minimal schema

### `workspaces`

| Column | Purpose |
| --- | --- |
| `id` | Primary key |
| `name` | Display name |
| `slug` | Stable, human-friendly unique identifier |
| `created_by` | User who created the workspace |
| `created_at`, `updated_at` | Audit timestamps |

### `workspace_memberships`

| Column | Purpose |
| --- | --- |
| `workspace_id` | Workspace foreign key |
| `user_id` | User foreign key |
| `role` | `OWNER`, `ADMIN`, `MEMBER`, or `VIEWER` |
| `created_at`, `updated_at` | Membership timestamps |

The primary key is `(workspace_id, user_id)`. Index `user_id` for listing a user's
workspaces.

### `documents`

Add `workspace_id`. Retain `uploaded_by`; it answers "who uploaded this?", while
`workspace_id` answers "who can access this?". Add an index beginning with
`workspace_id` for document and retrieval scopes.

Career matches do not need a workspace column in the first schema slice because their
two documents already determine the scope. The V1 workflow authorization layer must
require both documents to belong to the requested workspace. Add a direct
`workspace_id` later if agent/report queries need it for operational efficiency.

## Migration strategy

Do not edit existing Flyway migrations. Add the next migration only after the currently
uncommitted V7/V8 migrations are finalized.

1. Create `workspaces` and `workspace_memberships`.
2. Create one `Personal` workspace for every existing user.
3. Make each user the `OWNER` of that personal workspace.
4. Add nullable `documents.workspace_id`.
5. Backfill every document into its uploader's personal workspace.
6. Add the foreign key, make `workspace_id` non-null, and add retrieval/listing indexes.
7. Update new-user registration so it creates a personal workspace and owner membership
   in the same transaction.

The backfill is safe because every current document has a non-null `uploaded_by`.

## Authorization policy for the first implementation

| Operation | Minimum workspace role |
| --- | --- |
| List/query/read documents | `VIEWER` |
| Upload/delete documents | `MEMBER` |
| Manage collections and workspace knowledge settings | `ADMIN` |
| Invite/remove members, rename workspace | `OWNER` |

Global `ADMIN` bypasses workspace membership only on explicitly designated system-admin
routes. It must not silently bypass normal workspace routes.

## Delivery slices

### V1.1 -- Schema and personal-workspace backfill

- Add workspace domain entities, repositories, migration, and registration provisioning.
- Do not change existing RAG API routes yet.
- Verify existing personal documents remain visible through compatibility behavior.

Delivered:

- `V9__create_workspaces.sql` creates workspaces and memberships, creates one Personal
  workspace per existing user, adds an Owner membership, and backfills every document.
- New registrations receive their Personal workspace and Owner membership in the same
  transaction as user/credential/role creation.
- New uploads resolve the uploader's Personal workspace while existing endpoints remain
  uploader-scoped until V1.2.

### V1.2 -- Workspace access service and API

- Add a single workspace access service used by controllers/services.
- Add workspace list/create endpoints and workspace-scoped document endpoints.
- Transition retrieval and document management from uploader-scoped to workspace-scoped.
- Add authorization tests for all membership roles.

### V1.3 -- Collections and flexible source metadata

- Add collections inside workspaces.
- Replace career-specific core document typing with generic metadata/tags while retaining
  the career comparison workflow as a client of the generic model.

## Explicit deferrals

- Invitations, email delivery, SSO, organization billing, workspace deletion, and
  cross-workspace sharing are not part of V1.1.
- Collections are not needed to make workspace isolation correct.
- Agent runs, approval policies, and integrations wait until the workspace boundary is
  established.
