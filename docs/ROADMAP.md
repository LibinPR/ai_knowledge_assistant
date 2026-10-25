# Product Roadmap

## North star

Evolve AI Knowledge Assistant into a governed agentic knowledge platform. Users will
store approved knowledge, run evidence-backed workflows, inspect what the system did,
and approve consequential actions. The platform prioritizes reliable backend behavior,
source provenance, security, observability, and evaluation over novelty.

The current career-matching capability remains a reference workflow. It is not the
long-term product boundary.

## Engineering principles

- Keep a modular Spring Boot monolith until a concrete operational need proves otherwise.
- Make every AI capability bounded, observable, and testable.
- Prefer one well-instrumented workflow over premature multi-agent orchestration.
- Treat uploaded and connected content as untrusted input.
- Keep authorization and business rules on the backend.
- Deliver small vertical slices with migrations, tests, and documentation.

## Version plan

### V0.1 -- Baseline stabilization (complete)

Goal: verify and document the existing authenticated RAG system without expanding
product scope.

In scope:

- Preserve and assess the current hybrid retrieval and career-match work.
- Verify compilation and existing tests.
- Record verified behavior, constraints, and high-value test gaps.
- Establish durable project handoff documentation.

Out of scope:

- Workspaces, agent runtime, new integrations, frontend rewrite, or microservices.
- Rewriting the current RAG implementation.

Exit criteria:

- The baseline test command is documented and passing.
- Current RAG behavior and known risks are recorded.
- The next smallest reliability slice is selected.

### V0.2 -- Retrieval and query reliability (complete)

Goal: lock down the current hybrid retrieval and RAG-query behavior with focused
automated tests, without changing the product model.

Initial scope:

- owner isolation and ready-document retrieval constraints;
- hybrid-ranking and source/citation behavior;
- no-result and processing-pending query responses;
- career-report validation where it does not require external services.

Delivered:

- unit coverage for reciprocal-rank fusion, relevance filtering, and owner ID propagation;
- deterministic, deduplicated source ordering in RAG responses;
- no-evidence and processing-pending query behavior;
- repository query contracts for owner and ready-document scopes;
- career report JSON validation.

### V1 -- Knowledge platform core

Introduce workspaces, membership/RBAC, collections, and flexible source metadata.
Move career-specific assumptions out of core knowledge modelling while retaining career
matching as a workflow.

### V2 -- Durable ingestion

Add durable job state, retries, backoff, idempotency, diagnostics, source/extraction
versioning, embedding model tracking, and an object-storage abstraction.

### V3 -- Agent runtime foundation

Add bounded workflow execution, agent runs and steps, typed read-only tools, structured
outputs, artifacts, citations, and a trace timeline. The first flagship workflow is a
cited research brief.

### V4 -- Governance and approvals

Add action policies, approval requests, immutable audit events, source trust levels,
and approval-gated consequential tools.

### V5 -- Evaluation-driven AI

Add datasets, expected-source test cases, retrieval and grounding metrics, schema/tool
validity checks, and regression evaluation runs.

### V6 -- Integrations and advanced frontend

Add selected permissioned integrations and move the static frontend to a modern client
only after the backend contracts are stable.

### V7 -- Selective multi-agent workflows

Add distinct agents only when their separate responsibilities are demonstrably useful,
traceable, and evaluable.

## Explicit non-goals for now

- Microservices, Kafka/RabbitMQ, Kubernetes, or a separate vector database.
- Autonomous external writes without approval.
- Unbounded agent loops or multi-agent systems for presentation value alone.
- Replacing Java/Spring Boot simply because Python is common in AI projects.
