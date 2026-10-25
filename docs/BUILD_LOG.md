# Build Log

This file is the durable handoff between implementation sessions. Read it together with
`docs/ROADMAP.md`, `ARCHITECTURE.md`, and the current Git diff before starting work.

## V0.1 -- Baseline stabilization

Status: complete

### Verified on 2026-10-24

- `mvn test` succeeds with 5 tests: JWT secret parsing, file parsing, and text chunk
  metadata behavior.
- The current working tree contains uncommitted hybrid-retrieval, document-processing,
  source-provenance, and career-match changes. They are intentional in-progress work
  and must not be overwritten or discarded without explicit approval.
- `git diff --check` reported no whitespace errors.
- The Maven wrapper command (`.\\mvnw.cmd test`) failed in this PowerShell execution
  environment before Maven started. Direct Maven (`mvn test`) is the verified baseline
  command for now. Investigate the wrapper separately if it also fails in a normal
  developer terminal.

### Current functional baseline

- Authenticated users upload PDF, TXT, Markdown, and DOCX files.
- A scheduled processor parses, chunks, embeds, and indexes documents in PostgreSQL
  with pgvector.
- Document states are `QUEUED`, `PARSING`, `EMBEDDING`, `READY`, and `FAILED`.
- Retrieval combines vector similarity and PostgreSQL full-text search using reciprocal
  rank fusion.
- Retrieval is limited to ready documents owned by the authenticated user.
- RAG responses include document/page/section/excerpt source metadata.
- A career workflow compares an owned ready resume and job description and persists a
  validated structured report.

### Known risks and deliberate follow-up candidates

- Test coverage does not yet exercise repositories, authorization boundaries,
  migrations, document processing, hybrid ranking, or career-report validation.
- Scheduler polling is not a durable job queue and cannot safely coordinate multiple
  application instances. This belongs in V2, not a V0.1 rewrite.
- Original upload bytes are stored in PostgreSQL during processing. Move behind an
  object-storage abstraction in V2.
- RAG source responses are currently deduplicated by document, which can hide multiple
  relevant sections from one source document. Decide desired citation granularity and
  cover it with tests in a later V0.x reliability slice.
- The career generation transaction includes a remote LLM request. Refactor this before
  treating that workflow as production-ready so database transactions do not remain
  open during network calls.

### Next version: V0.2 -- Retrieval and query reliability

Add focused automated tests for the current hybrid-retrieval and RAG-query orchestration
without changing the product model. The tests should lock down owner isolation,
ready-document filtering, hybrid ranking/citation behavior, and the no-results response.

## V0.2 -- Retrieval and query reliability

Status: complete

### Delivered on 2026-10-25

- Added unit tests for reciprocal-rank fusion, semantic-score filtering, keyword-only
  candidates, and authenticated owner IDs passed to both retrieval queries.
- Added query-orchestration tests for pending documents, no-evidence responses,
  evidence passed to the model, and source response behavior.
- Preserved sorted source order after deduplicating sources by document. The previous
  `HashMap` collector could return a different order from the hybrid-score order.
- Added repository query contract tests that guard the required owner and `READY`
  document conditions. These do not replace a future PostgreSQL integration test.
- Added isolated tests for valid and invalid career-match structured JSON.
- `mvn -B test` passes with 13 tests and no failures.

### Build note

- Mockito emits a JDK warning about dynamically attaching its inline mock-maker agent.
  It does not fail the build. Configure Mockito as an explicit test JVM agent when the
  project next updates test-build tooling.

### Recommended next version

Begin V1 -- Knowledge platform core. Start with a small design-only slice for workspace
and membership modelling before changing the existing per-user document model.

## V1 -- Knowledge platform core

Status: workspace design proposed

### Design artefact

- `docs/V1_WORKSPACE_DESIGN.md` defines the initial workspace boundary, global versus
  workspace roles, migration/backfill strategy, authorization policy, and staged
  delivery plan.

### V1.1 -- Schema and personal-workspace backfill

Status: complete

### Delivered on 2026-10-25

- Added the `workspaces` and `workspace_memberships` tables in V9, with a safe Personal
  workspace and Owner membership backfill for existing users/documents.
- Added workspace and membership JPA domain/repository types.
- Added `documents.workspace_id`; document uploads now assign the uploader's Personal
  workspace without changing existing API routes.
- New registrations provision a Personal workspace and Owner membership in the same
  transaction.
- Added a registration unit test for workspace provisioning.
- `mvn -B test` passes with 14 tests and no failures.

### Next version: V1.2 -- Workspace access service and API

Add one reusable membership/permission service, workspace list/create endpoints, and
explicit workspace-scoped document/query routes. Existing `/api/rag/**` routes should
be retired only after the new routes have authorization coverage.
