# AI Knowledge Assistant

A production-ready **RAG (Retrieval-Augmented Generation)** backend built with Spring Boot 3.5. Upload documents, ask questions, and get AI-powered answers grounded in your own content.

---

## Features

- **JWT Authentication** — register, login, refresh tokens, logout single/all devices
- **Token versioning** — invalidate all sessions on password change or forced logout
- **Document ingestion** — upload PDF or TXT files, auto-chunked and embedded
- **Vector similarity search** — pgvector-powered semantic search over document chunks
- **RAG query endpoint** — questions answered by Groq LLM using retrieved context
- **Document management** — list and delete documents (role-aware: admin sees all)
- **Flyway migrations** — full schema versioning from V1 to V5
- **Role-based access** — USER and ADMIN roles with Spring Security

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENT (HTTP)                            │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                   SPRING BOOT 3.5 (port 8080)                   │
│                                                                 │
│  JwtAuthenticationFilter → extracts userId (Long) as principal  │
│                                                                 │
│  ┌──────────────────┐    ┌──────────────────────────────────┐   │
│  │  Auth Layer      │    │  RAG Layer                       │   │
│  │  /api/auth/**    │    │  /api/rag/**                     │   │
│  │                  │    │                                  │   │
│  │  register        │    │  POST /ingest   → file upload    │   │
│  │  login           │    │  POST /query    → ask question   │   │
│  │  refresh         │    │  GET  /documents → list docs     │   │
│  │  logout          │    │  DELETE /documents/{id}          │   │
│  │  logout-all      │    │                                  │   │
│  └──────────────────┘    └──────────────────────────────────┘   │
└──────────────────────────────────┬──────────────────────────────┘
                                   │
              ┌────────────────────┼────────────────────┐
              │                    │                    │
              ▼                    ▼                    ▼
  ┌─────────────────┐   ┌──────────────────┐  ┌──────────────────┐
  │  PostgreSQL 16  │   │ Embedding Server │  │   Groq API       │
  │  + pgvector     │   │ localhost:8081   │  │ llama-3.1-8b     │
  │  localhost:5433 │   │ (Docker/HF TEI)  │  │ -instant         │
  └─────────────────┘   └──────────────────┘  └──────────────────┘
```

---

## RAG Pipeline

```
User uploads file
       │
       ▼
FileParser (PDF → text, TXT → text)
       │
       ▼
TextChunker (500 char chunks, 100 char overlap, sentence-boundary aware)
       │
       ▼
EmbeddingClient → POST http://localhost:8081/embed → Float[384]
       │
       ▼
DocumentChunk saved to PostgreSQL (content + vector(384))
```

```
User asks question
       │
       ▼
EmbeddingClient → embed the question → Float[384]
       │
       ▼
pgvector similarity search (cosine distance <=>)  → top 5 chunks
       │
       ▼
GroqClient → system prompt with context + user question
       │
       ▼
LLM answer returned to user
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.10 |
| Security | Spring Security + JWT (jjwt 0.12.5) |
| Persistence | Spring Data JPA + Hibernate 6 |
| Database | PostgreSQL 16 + pgvector extension |
| Migrations | Flyway 11 |
| Embedding Server | HuggingFace Text Embeddings Inference (Docker) |
| LLM | Groq API (llama-3.1-8b-instant) |
| PDF Parsing | Apache PDFBox 3 |
| Build | Maven |

---

## Database Schema

```
V1 — Auth tables
  users, roles, user_roles, user_credentials, refresh_tokens, audit_logs

V2 — Seed data
  INSERT roles: USER, ADMIN

V3 — Refresh token refactor
  DROP token column, ADD jti (UUID), ADD version (optimistic lock)

V4 — Token versioning
  ADD token_version to users

V5 — RAG tables
  documents (id, filename, uploaded_by → users, uploaded_at)
  document_chunks (id, document_id → documents, content TEXT, embedding vector(384))
```

---

## Prerequisites

| Requirement | Version |
|---|---|
| Java | 21 |
| Maven | 3.9+ |
| Docker | Latest |
| PostgreSQL | 16 (via Docker) |

---

## Setup & Running

### 1. Clone the repo

```bash
git clone https://github.com/yourusername/ai-knowledge-assistant.git
cd ai-knowledge-assistant
```

### 2. Start Docker containers

```bash
# PostgreSQL with pgvector
docker run -d \
  --name ai_knowledge_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=yourpassword \
  -e POSTGRES_DB=ai_knowledge_assistant \
  -p 5433:5432 \
  pgvector/pgvector:pg16

# HuggingFace Text Embeddings Inference (embedding server)
docker run -d \
  --name embedder \
  -p 8081:80 \
  ghcr.io/huggingface/text-embeddings-inference:cpu-latest \
  --model-id BAAI/bge-small-en-v1.5
```

### 3. Configure `application.yaml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/ai_knowledge_assistant
    username: postgres
    password: yourpassword

groq:
  api-key: "your-groq-api-key"
  base-url: "https://api.groq.com/openai/v1"
  model: "llama-3.1-8b-instant"
```

Get a free Groq API key at [console.groq.com](https://console.groq.com)

### 4. Build and run

```bash
mvn package -DskipTests
java -Xmx512m -Xms512m -XX:+UseSerialGC -XX:MaxRAMPercentage=80 \
  -jar target/ai-knowledge-assistant-0.0.1-SNAPSHOT.jar
```

The app starts on `http://localhost:8080`

On first startup with `dev` profile, an admin user is seeded:
- Email: `admin@local.dev`
- Password: `Libin@010`

---

## API Reference

### Auth

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | None | Register new user |
| POST | `/api/auth/login` | None | Login, returns JWT tokens |
| POST | `/api/auth/refresh` | None | Refresh access token |
| POST | `/api/auth/logout` | Bearer | Revoke refresh token |
| POST | `/api/auth/logout-all` | Bearer | Revoke all sessions |

### RAG

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/rag/ingest` | Bearer | Upload PDF/TXT file |
| POST | `/api/rag/query` | Bearer | Ask a question |
| GET | `/api/rag/documents` | Bearer | List documents |
| DELETE | `/api/rag/documents/{id}` | Bearer | Delete a document |

### Example: Ingest a file

```
POST /api/rag/ingest
Authorization: Bearer <token>
Content-Type: multipart/form-data

file: sample.txt
```

### Example: Query

```
POST /api/rag/query
Authorization: Bearer <token>
Content-Type: text/plain

What is machine learning?
```

---

## Project Structure

```
src/main/java/com/yourorg/aiknowledgeassistant/
├── auth/                          # JWT service, auth service, refresh token
├── bootstrap/                     # DevBootstrap — seeds admin on dev startup
├── config/                        # Security config, beans config
├── controller/
│   ├── dto/                       # DocumentResponse record
│   └── RagController.java         # All RAG endpoints
├── domain/
│   ├── user/                      # User, Role, UserRole, UserCredentials
│   └── rag/                       # Document, DocumentChunk
├── exception/                     # GlobalExceptionHandler
├── repository/
│   ├── user/                      # Auth repositories
│   └── rag/                       # DocumentRepository, DocumentChunkRepository
└── service/
    ├── audit/                     # AuditService
    ├── security/jwt/              # JwtAuthenticationFilter
    ├── user/                      # UserService
    └── rag/                       # DocumentService, TextChunker, FileParser,
                                   # EmbeddingClient, SimilaritySearchService,
                                   # RagQueryService, GroqClient,
                                   # DocumentManagementService

src/main/resources/
├── application.yaml
└── db/migration/
    ├── V1__init_schema.sql
    ├── V2__seed_roles.sql
    ├── V3__refactor_refresh_token.sql
    ├── V4__add_token_version.sql
    └── V5__create_rag_tables.sql
```

---

## Environment Variables

| Variable | Description |
|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | DB password |
| `SECURITY_JWT_SECRET` | Base64-encoded JWT signing secret |
| `GROQ_API_KEY` | Groq API key |
| `GROQ_MODEL` | LLM model name |

---

## Low Memory Environments

On machines with less than 4GB RAM, run with:

```bash
java -Xmx384m -Xms384m -XX:+UseSerialGC -XX:MaxRAMPercentage=80 \
  -jar target/ai-knowledge-assistant-0.0.1-SNAPSHOT.jar
```

Also limit Docker/WSL2 memory via `~/.wslconfig`:

```ini
[wsl2]
memory=1GB
swap=512MB
```

---

## Security Notes

- Passwords are BCrypt hashed
- JWT access tokens expire in 15 minutes
- Refresh tokens are stored as JTI (UUID) in DB, not the full token
- Token versioning invalidates all tokens on forced logout
- CSRF disabled (stateless JWT API)
- `/api/auth/**` is public; all other endpoints require authentication

---

## Roadmap

- [ ] Secure RAG endpoints (currently `permitAll`)
- [ ] RAG query history per user
- [ ] Multi-chunk ingestion per request
- [ ] File size validation
- [ ] Docker Compose setup
- [ ] Frontend UI
