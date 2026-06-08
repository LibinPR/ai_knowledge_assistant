# AI Knowledge Assistant Architecture

## Overview

AI Knowledge Assistant is a Java 21 Spring Boot 3.5 application that provides authenticated document upload, semantic search, and Retrieval-Augmented Generation (RAG). Users upload documents, the system extracts text, creates embeddings, stores vectors in PostgreSQL with pgvector, and answers questions using retrieved document context with the Groq API.

## High-Level Design

```text
|----------------------|
|      User Browser    |
|----------------------|
           |
           | HTTP
           v
|----------------------|
|   Static Web UI      |
|   index.html         |
|----------------------|
           |
           | REST API + Bearer JWT
           v
|------------------------------------------------------------------|
|                       Spring Boot Application                    |
|                       Port: 8080                                 |
|------------------------------------------------------------------|
| SecurityFilterChain | JwtAuthenticationFilter | RateLimitFilter   |
|---------------------|-------------------------|------------------|
| AuthController      | RagController           | AdminController  |
| /api/auth/**        | /api/rag/**             | /api/admin/**    |
|---------------------|-------------------------|------------------|
           |                    |                       |
           v                    v                       v
|------------------|   |----------------------|     |------------------|
| Auth Services    |   | RAG Services         |     | Admin Service    |
| UserService      |   | DocumentService      |     | User/doc stats   |
| JwtService       |   | FileParser           |     |                  |
| Refresh tokens   |   | RagQueryService      |     |                  |
|------------------|   |----------------------|     |------------------|
           |                    |                       |
           v                    v                       v
|------------------|   |----------------------|     |------------------|
| PostgreSQL       |   | Embedding Server     |     | PostgreSQL       |
| users            |   | Port: 8081           |     | aggregate counts |
| roles            |   | POST /embed          |     |                  |
| refresh_tokens   |   | BAAI/bge-small       |     |                  |
| audit_logs       |   |----------------------|     |------------------|
|------------------|              |
                              v
                    |------------------------|
                    | PostgreSQL + pgvector  |
                    | documents              |
                    | document_chunks        |
                    | embedding vector(384)  |
                    |------------------------|
                              |
                              v
                    |------------------------|
                    | Groq API               |
                    | OpenAI-compatible LLM  |
                    |------------------------|
```

## Low-Level Component Design

```text
|------------------------------------------------------------------|
| com.yourorg.aiknowledgeassistant                                |
|------------------------------------------------------------------|
| auth                                                             |
|   AuthController                                                 |
|   AuthenticationService                                          |
|   JwtService                                                     |
|   RefreshTokenService                                            |
|   dto: LoginRequest, LoginResponse, RegisterRequest, ...         |
|------------------------------------------------------------------|
| config                                                           |
|   SecurityConfig                                                 |
|   JwtConfig                                                      |
|   RateLimitFilter                                                |
|   SecurityBeansConfig                                            |
|------------------------------------------------------------------|
| controller                                                       |
|   RagController                                                  |
|   AdminController                                                |
|   UserController                                                 |
|   TestController                                                 |
|------------------------------------------------------------------|
| service.rag                                                      |
|   FileParser                                                     |
|   TextChunker                                                    |
|   DocumentService                                                |
|   DocumentManagementService                                      |
|   EmbeddingClient                                                |
|   SimilaritySearchService                                        |
|   RagQueryService                                                |
|   GroqClient                                                     |
|------------------------------------------------------------------|
| repository                                                       |
|   user: UserRepository, RoleRepository, RefreshTokenRepository   |
|   rag: DocumentRepository, DocumentChunkRepository               |
|   audit: AuditRepository                                         |
|------------------------------------------------------------------|
| domain                                                           |
|   user: User, Role, UserRole, UserCredentials, RefreshToken      |
|   rag: Document, DocumentChunk                                   |
|   audit: AuditLog                                                |
|------------------------------------------------------------------|
```

## Request Flow

### Authentication Flow

```text
|----------------------|
| User enters email    |
| and password         |
|----------------------|
           |
           v
|----------------------|
| POST /api/auth/login |
| AuthController       |
|----------------------|
           |
           v
|--------------------------|
| AuthenticationService    |
| Loads user credentials   |
| Checks password hash     |
| Loads assigned roles     |
|--------------------------|
           |
           v
|--------------------------|
| JwtService               |
| Creates access token     |
| Creates refresh token    |
| Includes userId, roles   |
| and token version        |
|--------------------------|
           |
           v
|--------------------------|
| RefreshTokenService      |
| Stores refresh token JTI |
| expiration, revoked flag |
|--------------------------|
           |
           v
|--------------------------|
| LoginResponse            |
| accessToken              |
| refreshToken             |
|--------------------------|
```

### Authenticated Request Flow

```text
|--------------------------|
| Browser sends request    |
| Authorization: Bearer    |
|--------------------------|
           |
           v
|--------------------------|
| JwtAuthenticationFilter  |
| Parses access token      |
| Validates signature      |
| Checks token version     |
|--------------------------|
           |
           v
|--------------------------|
| SecurityContext          |
| principal = userId       |
| authorities = roles      |
|--------------------------|
           |
           v
|--------------------------|
| Controller method        |
| Receives authenticated   |
| user context             |
|--------------------------|
```

### Document Ingestion Flow

```text
|--------------------------|
| User uploads document    |
| PDF, TXT, MD, DOCX       |
|--------------------------|
           |
           v
|--------------------------|
| POST /api/rag/ingest     |
| MultipartFile + JWT      |
|--------------------------|
           |
           v
|--------------------------|
| RagController            |
| Reads userId from        |
| AuthenticationPrincipal  |
|--------------------------|
           |
           v
|--------------------------|
| FileParser               |
| PDFBox for PDF           |
| Apache POI for DOCX      |
| Plain text for TXT/MD    |
|--------------------------|
           |
           v
|--------------------------|
| DocumentService          |
| Creates Document         |
| Splits content into      |
| searchable chunks        |
|--------------------------|
           |
           v
|--------------------------|
| EmbeddingClient          |
| POST /embed              |
| embedding server :8081   |
|--------------------------|
           |
           v
|--------------------------|
| DocumentChunkRepository  |
| Saves content + vector   |
| in pgvector column       |
|--------------------------|
```

### Query Flow

```text
|--------------------------|
| User asks a question     |
|--------------------------|
           |
           v
|--------------------------|
| POST /api/rag/query      |
| question query parameter |
|--------------------------|
           |
           v
|--------------------------|
| RagQueryService          |
| Coordinates retrieval    |
| and answer generation    |
|--------------------------|
           |
           v
|--------------------------|
| EmbeddingClient          |
| Embeds the question      |
|--------------------------|
           |
           v
|--------------------------|
| SimilaritySearchService  |
| Searches pgvector        |
| for top matching chunks  |
|--------------------------|
           |
           v
|--------------------------|
| GroqClient               |
| Sends prompt containing  |
| context + question       |
|--------------------------|
           |
           v
|--------------------------|
| QueryResponse            |
| answer                   |
| source document chunks   |
|--------------------------|
```

## Data Model

```text
|------------------|       |------------------|       |------------------|
| users            |       | user_roles       |       | roles            |
|------------------|       |------------------|       |------------------|
| id PK            |<----->| user_id FK       |       | id PK            |
| email UNIQUE     |       | role_id FK       |<----->| name UNIQUE      |
| full_name        |       |------------------|       | description      |
| enabled          |                                  |------------------|
| locked           |
| token_version    |
| created_at       |
| updated_at       |
|------------------|
        |
        v
|------------------|       |------------------|
| user_credentials |       | refresh_tokens   |
|------------------|       |------------------|
| user_id PK/FK    |       | id PK            |
| password_hash    |       | user_id FK       |
|------------------|       | jti UNIQUE       |
                           | expires_at       |
                           | revoked          |
                           | version          |
                           |------------------|

|------------------|       |------------------------|
| documents        |       | document_chunks        |
|------------------|       |------------------------|
| id PK            |<----->| id PK                  |
| filename         |       | document_id FK         |
| uploaded_by FK   |       | content TEXT           |
| uploaded_at      |       | embedding vector(384)  |
|------------------|       |------------------------|

|------------------|
| audit_logs       |
|------------------|
| id PK            |
| actor_user_id FK |
| action           |
| entity           |
| entity_id        |
| metadata         |
|------------------|
```

## Database Migrations

| Version | File | Purpose |
| --- | --- | --- |
| V1 | `V1__init_schema.sql` | Initial auth and audit schema |
| V2 | `V2__seed_roles.sql` | Seeds `USER` and `ADMIN` roles |
| V3 | `V3__refactor_refersh_token.sql` | Refactors refresh token storage to JTI |
| V4 | `V4__add_token_version.sql` | Adds token versioning to users |
| V5 | `V5__create_rag_files.sql` | Adds document and chunk storage for RAG |

## Deployment Topology

```text
|---------------------------- docker compose ----------------------------|
|                                                                       |
|  |----------------------|      |----------------------|                |
|  | app                  |      | db                   |                |
|  | Spring Boot :8080    |----->| pgvector/pgvector:16 |                |
|  | profile: prod        |      | internal :5432       |                |
|  |----------------------|      | external :5433       |                |
|             |                  |----------------------|                |
|             |                                                         |
|             v                                                         |
|  |----------------------|                                            |
|  | embedder             |                                            |
|  | HF TEI CPU image     |                                            |
|  | internal :80         |                                            |
|  | external :8081       |                                            |
|  |----------------------|                                            |
|                                                                       |
|------------------------------------------------------------------------|

External service:

|----------------------|
| Groq API             |
| base URL configured  |
| by GROQ_BASE_URL     |
|----------------------|
```

## Runtime Configuration

| Variable | Purpose | Default |
| --- | --- | --- |
| `DB_URL` | JDBC database URL | `jdbc:postgresql://localhost:5433/ai_knowledge_assistant` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | local development value in `application.yaml` |
| `JWT_SECRET` | JWT signing secret | development fallback in `application.yaml` |
| `GROQ_API_KEY` | Groq API key | development fallback in `application.yaml` |
| `GROQ_BASE_URL` | Groq OpenAI-compatible base URL | `https://api.groq.com/openai/v1` |
| `GROQ_MODEL` | Groq model name | `llama-3.1-8b-instant` |
| `EMBEDDING_SERVER_URL` | Embedding endpoint | `http://localhost:8081/embed` |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `dev` |

## Security Boundaries

| Area | Behavior |
| --- | --- |
| Public routes | `/`, `/index.html`, static assets, `/api/auth/**`, `/api/test/**` |
| Authenticated routes | `/api/rag/**` |
| User/Admin routes | `/api/user/**` |
| Admin-only routes | `/api/admin/**` |
| Token strategy | Short-lived access token plus stored refresh token JTI |
| Session model | Stateless API requests with JWT validation per request |
| Rate limiting | `RateLimitFilter` runs after JWT authentication |

## Key Design Notes

- The browser UI is static and served by the same Spring Boot app, so it uses `window.location.origin` for API calls.
- PostgreSQL stores both relational auth data and vector-search data.
- pgvector keeps semantic search inside the database instead of requiring a separate vector database.
- The embedding server is a separate runtime service so embedding model concerns stay outside the Spring Boot process.
- Groq is called only after relevant chunks are retrieved, so answers are grounded in uploaded documents.
- Admin users can access aggregate stats and broader document management behavior.

## Future Enhancements

- Background ingestion jobs for large documents.
- Streaming responses for long LLM answers.
- Conversation history and follow-up question memory.
- S3 or object storage for original uploaded files.
- Redis-backed distributed rate limiting.
- Hybrid search with keyword and vector ranking.
- Citation highlighting in the UI.
