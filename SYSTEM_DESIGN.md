# System Design - AI Knowledge Assistant

## Overview

AI Knowledge Assistant is a Retrieval-Augmented Generation (RAG) application that allows users to upload documents and ask natural language questions against their private knowledge base.

The system combines semantic search using vector embeddings with Large Language Models (LLMs) to generate context-aware answers grounded in uploaded documents.

---

# High Level Architecture

```text
User
 │
 ▼
Frontend (HTML/CSS/JS)
 │
 ▼
Spring Boot Backend
 │
 ├──────────────► PostgreSQL + pgvector
 │                     │
 │                     ▼
 │               Document Chunks
 │               Vector Embeddings
 │
 ├──────────────► HuggingFace TEI
 │                     │
 │                     ▼
 │              BAAI/bge-small-en-v1.5
 │
 └──────────────► Groq LLM
                       │
                       ▼
                 Final Answer
```

---

# Core Components

## Frontend

Responsibilities:

* User Registration
* User Login
* Document Upload
* Ask Questions
* Display Answers
* Display Sources
* Admin Dashboard

Technology:

* HTML
* CSS
* Vanilla JavaScript

---

## Spring Boot Backend

Responsibilities:

* Authentication
* Authorization
* File Upload
* Document Processing
* Embedding Generation
* Semantic Search
* RAG Orchestration

Technology:

* Java 21
* Spring Boot 3.5
* Spring Security
* JWT
* Flyway

---

## PostgreSQL + pgvector

Responsibilities:

* Store users
* Store documents
* Store chunks
* Store embeddings
* Perform vector similarity search

Technology:

* PostgreSQL
* pgvector

Similarity Metric:

* Cosine Similarity

---

## HuggingFace Text Embeddings Inference (TEI)

Responsibilities:

* Generate embeddings for:

  * Uploaded document chunks
  * User queries

Model:

* BAAI/bge-small-en-v1.5

Embedding Dimension:

* 384

---

## Groq LLM

Responsibilities:

* Generate final answers
* Use retrieved document context
* Reduce hallucinations using prompt constraints

Model:

* llama-3.1-8b-instant

---

# Document Ingestion Flow

```text
User Uploads Document
        │
        ▼
Extract Text
        │
        ▼
Split Into Chunks
        │
        ▼
Generate Embeddings
        │
        ▼
Store Chunks + Embeddings
        │
        ▼
PostgreSQL + pgvector
```

Supported Formats:

* PDF
* DOCX
* TXT
* MD

---

# Query Flow

```text
User Question
      │
      ▼
Generate Query Embedding
      │
      ▼
Vector Similarity Search
      │
      ▼
Top K Chunks Retrieved
      │
      ▼
Build Context
      │
      ▼
Send Context + Question
      │
      ▼
Groq LLM
      │
      ▼
Answer + Sources
```

---

# Security Design

## Authentication

Implemented using:

* JWT Access Tokens
* Refresh Tokens

Flow:

```text
Login
 │
 ▼
Access Token
Refresh Token
 │
 ▼
Authenticated APIs
```

---

## Authorization

Roles:

### USER

Permissions:

* Upload Documents
* Delete Own Documents
* Query Own Documents

### ADMIN

Permissions:

* View System Statistics
* Manage Users
* Access All Documents

---

## User Isolation

Regular users can only retrieve:

```sql
uploaded_by = current_user_id
```

This prevents users from accessing documents uploaded by other users.

---

# Database Design

## users

Stores application users.

Fields:

* id
* email
* password
* role

---

## documents

Stores uploaded documents.

Fields:

* id
* filename
* uploaded_by
* uploaded_at

---

## document_chunks

Stores chunked content and embeddings.

Fields:

* id
* document_id
* content
* embedding

Embedding Type:

```sql
vector(384)
```

---

# Retrieval Strategy

Embedding Model:

* BAAI/bge-small-en-v1.5

Top K:

```text
3
```

Similarity Threshold:

```text
0.60
```

Ranking:

```text
Cosine Similarity
```

---

# Rate Limiting

Implemented using:

* Bucket4j

Protected Endpoint:

```text
/api/rag/**
```

Strategy:

* User based throttling
* Prevent abuse of LLM resources

---

# Deployment Architecture

```text
Railway
 │
 ├── Spring Boot Application
 │
 ├── PostgreSQL Database
 │
 └── HuggingFace TEI Service
```

Containerization:

* Docker

Build Tool:

* Maven

---

# Design Tradeoffs

## Why pgvector?

Pros:

* Simple architecture
* No separate vector database
* Easier deployment
* Lower operational complexity

Cons:

* Less scalable than dedicated vector databases

---

## Why BGE Small?

Pros:

* Fast inference
* Low memory usage
* Good retrieval quality

Cons:

* Lower semantic accuracy than larger embedding models

---

## Why Groq?

Pros:

* Low latency
* Simple OpenAI-compatible API
* Cost effective

---

# Future Improvements

* Hybrid Search (Keyword + Vector)
* Reranking Layer
* Streaming Responses
* Chat History
* Multi-document Conversations
* S3 File Storage
* Observability & Metrics
* Kubernetes Deployment
* Distributed Rate Limiting

---

# Scalability Considerations

Current system is suitable for:

* Portfolio Projects
* Internal Knowledge Bases
* Small Team Deployments

For large-scale deployments:

* Move embeddings to dedicated service
* Introduce caching
* Add reranking
* Use object storage
* Scale database separately

---

# Conclusion

The AI Knowledge Assistant demonstrates a production-oriented Retrieval-Augmented Generation architecture using Spring Boot, PostgreSQL, pgvector, HuggingFace embeddings, Groq LLMs, JWT authentication, RBAC, Docker, and Railway deployment.
