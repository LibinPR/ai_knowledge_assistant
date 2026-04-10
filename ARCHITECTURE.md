# AI Knowledge Assistant — Architecture Document

## Project Overview

A Spring Boot 3.5 application providing a **RAG (Retrieval-Augmented Generation) pipeline** with JWT-based authentication. Users upload documents which are chunked, embedded via a local embedding server, and stored in PostgreSQL with the pgvector extension for semantic similarity search.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.10 |
| Security | Spring Security + JWT (jjwt 0.12.5) |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL (port 5433) + pgvector extension |
| Schema Migration | Flyway |
| Embedding Server | Local Python server (port 8081) |
| HTTP Client | RestTemplate (sync) / WebClient (async, unused) |
| Build | Maven |

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENT (HTTP)                            │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                   SPRING BOOT APPLICATION                       │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                  Security Layer                          │   │
│  │   JwtAuthenticationFilter → parses Bearer token         │   │
│  │   Sets principal = user.getId() (Long) in SecurityCtx   │   │
│  └─────────────────────────┬───────────────────────────────┘   │
│                            │                                    │
│  ┌─────────────────────────▼───────────────────────────────┐   │
│  │                   Controllers                            │   │
│  │                                                         │   │
│  │   AuthController        /api/auth/**                    │   │
│  │   RagController         /api/rag/ingest                 │   │
│  └──────────┬──────────────────────────┬───────────────────┘   │
│             │                          │                        │
│  ┌──────────▼──────────┐  ┌────────────▼──────────────────┐   │
│  │    Auth Services     │  │       RAG Services             │   │
│  │                      │  │                                │   │
│  │  UserService         │  │  DocumentService               │   │
│  │  - registerUser()    │  │  - ingest(filename,            │   │
│  │  - assignRole()      │  │          content, userId)      │   │
│  │                      │  │                                │   │
│  │  JwtService          │  │  EmbeddingClient               │   │
│  │  - parseAccessToken()│  │  - embed(text) → Float[]       │   │
│  │  - generateTokens()  │  │  - calls localhost:8081/embed  │   │
│  └──────────┬───────────┘  └────────────┬──────────────────┘   │
│             │                           │                       │
│  ┌──────────▼───────────────────────────▼───────────────────┐  │
│  │                    Repositories                           │  │
│  │                                                          │  │
│  │  UserRepository       DocumentRepository                 │  │
│  │  RoleRepository       DocumentChunkRepository            │  │
│  │  UserRoleRepository                                      │  │
│  │  UserCredentialsRepository                               │  │
│  │  RefreshTokenRepository                                  │  │
│  └──────────────────────────┬───────────────────────────────┘  │
└─────────────────────────────┼───────────────────────────────────┘
                              │
              ┌───────────────┼───────────────────┐
              │               │                   │
              ▼               ▼                   ▼
  ┌──────────────────┐  ┌──────────┐   ┌──────────────────────┐
  │   PostgreSQL DB   │  │ Flyway   │   │  Local Embedding     │
  │   port 5433       │  │Migrations│   │  Server port 8081    │
  │                   │  │          │   │  POST /embed         │
  │  Auth Tables:     │  │ V1 schema│   │  input: text         │
  │  - users          │  │ V2 roles │   │  output: Float[384]  │
  │  - roles          │  │ V3 token │   └──────────────────────┘
  │  - user_roles     │  │ V4 ver   │
  │  - user_creds     │  │ V5 rag   │
  │  - refresh_tokens │  └──────────┘
  │  - audit_logs     │
  │                   │
  │  RAG Tables:      │
  │  - documents      │
  │  - document_chunks│
  │    (vector(384))  │
  └──────────────────┘
```

---

## Database Schema

### Auth Tables (V1–V4)

```
users
├── id (BIGSERIAL PK)
├── email (UNIQUE)
├── full_name
├── enabled
├── locked
├── token_version (INTEGER) ← V4
├── created_at
└── updated_at

roles
├── id (BIGSERIAL PK)
├── name (UNIQUE) ← seeded: USER, ADMIN
└── description

user_roles (join table)
├── user_id (FK → users)
└── role_id (FK → roles)

user_credentials
├── user_id (PK, FK → users)
└── password_hash

refresh_tokens
├── id (BIGSERIAL PK)
├── user_id (FK → users)
├── jti (UNIQUE) ← V3
├── expires_at
├── revoked
└── version (optimistic lock)

audit_logs
├── id
├── actor_user_id (FK → users)
├── action
├── entity / entity_id
└── metadata
```

### RAG Tables (V5)

```
documents
├── id (BIGSERIAL PK)
├── filename
├── uploaded_by (FK → users)
└── uploaded_at

document_chunks
├── id (BIGSERIAL PK)
├── document_id (FK → documents)
├── content (TEXT)
└── embedding (vector(384))  ← pgvector
```

---

## Domain Package Structure

```
com.yourorg.aiknowledgeassistant
├── auth/
│   └── JwtService.java
├── bootstrap/
│   └── DevBootstrap.java          ← seeds admin user on dev startup
├── config/
│   └── SecurityBeansConfig.java
├── controller/
│   ├── AuthController.java
│   └── RagController.java
├── domain/
│   ├── user/
│   │   ├── User.java
│   │   ├── Role.java
│   │   ├── UserRole.java
│   │   └── UserCredentials.java
│   └── rag/
│       ├── Document.java
│       └── DocumentChunk.java
├── repository/
│   ├── user/
│   │   ├── UserRepository.java
│   │   ├── RoleRepository.java
│   │   ├── UserRoleRepository.java
│   │   ├── UserCredentialsRepository.java
│   │   └── RefreshTokenRepository.java
│   └── rag/
│       ├── DocumentRepository.java
│       └── DocumentChunkRepository.java
├── service/
│   ├── user/
│   │   └── UserService.java
│   ├── audit/
│   │   └── AuditService.java
│   ├── security/jwt/
│   │   └── JwtAuthenticationFilter.java
│   └── rag/
│       ├── DocumentService.java
│       ├── EmbeddingClient.java    ← ACTIVE: calls local server
│       ├── EmbeddingService.java   ← INACTIVE: HuggingFace (not wired)
│       └── EmbeddingResponse.java  ← INACTIVE: unused record
└── exception/
    └── EmailAlreadyExistsException.java
```

---

## Flyway Migration History

| Version | File | Description |
|---|---|---|
| V1 | `V1__init_schema.sql` | All auth tables |
| V2 | `V2__seed_roles.sql` | Insert USER and ADMIN roles |
| V3 | `V3__refactor_refresh_token.sql` | Replace token col with JTI |
| V4 | `V4__add_token_version.sql` | Add token_version to users |
| V5 | `V5__create_rag_tables.sql` | documents + document_chunks |

---

## Security Flow

```
POST /api/auth/login
        │
        ▼
  JwtService.generateTokens()
        │
        ├── access token  (TTL: 900s)
        └── refresh token (TTL: 604800s, stored as JTI in DB)

Subsequent requests:
  Authorization: Bearer <access_token>
        │
        ▼
  JwtAuthenticationFilter
        ├── parse token → extract userId, roles, version
        ├── load User from DB
        ├── check token_version matches
        └── set principal = userId (Long) in SecurityContext
```

---

## RAG Ingest Flow

```
POST /api/rag/ingest?filename=myfile.txt
  Body: raw text content
  Auth: Bearer token
        │
        ▼
  RagController.ingest()
  extracts userId from @AuthenticationPrincipal (Long)
        │
        ▼
  DocumentService.ingest(filename, content, userId)
        ├── load User from DB
        ├── save Document (filename + user + timestamp)
        ├── EmbeddingClient.embed(content)
        │       └── POST http://localhost:8081/embed
        │               input:  { "inputs": "query:<content>" }
        │               output: List<List<Double>> → Float[384]
        └── save DocumentChunk (document + content + embedding)
```

---

## Known TODOs / Next Steps

- **Text chunking** — currently entire content is one chunk; need to split large documents into overlapping chunks before embedding
- **Similarity search** — need a `findSimilar(Float[] queryEmbedding)` query using pgvector's `<=>` operator in `DocumentChunkRepository`
- **RAG query endpoint** — `POST /api/rag/query` that embeds the question, retrieves top-K chunks, and sends to an LLM
- **EmbeddingService cleanup** — `EmbeddingService.java` and `EmbeddingTestController.java` need `@Service` / `@RestController` removed to prevent Spring wiring failures
- **Multi-chunk ingestion** — `ingest()` should accept a list of chunks per document, not just one string
- **File upload** — replace raw `@RequestBody String` with `MultipartFile` for actual PDF/TXT uploads
