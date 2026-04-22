# ADR 003: PostgreSQL vs MongoDB for User Data

## Status
Accepted

## Context
The User API needs persistent storage for user entities with the following characteristics:
- Structured user data (username, email, phone, timestamps)
- ACID compliance for user mutations
- Complex query support (search by username, pagination)
- Future audit logging and compliance requirements

### Requirements
- Relational data with foreign keys (future: user preferences, audit logs)
- Complex querying (LIKE searches, sorting, pagination)
- Transactional integrity for user updates
- Mature ORM support (Spring Data JPA)
- Docker-friendly local development

## Decision
Use **PostgreSQL** as the primary database.

### Why PostgreSQL

| Criteria | PostgreSQL | MongoDB | MySQL | H2 (embedded) |
|----------|------------|---------|-------|---------------|
| **ACID Transactions** | ✅ Full support | ✅ Multi-doc (4.0+) | ✅ | ✅ |
| **Complex Queries** | ✅ Advanced SQL, JOINs | ⚠️ Aggregation pipeline | ✅ | ✅ |
| **Full-Text Search** | ✅ tsvector, GIN indexes | ✅ Text indexes | ⚠️ Basic | ❌ |
| **JSON Support** | ✅ JSONB (indexed) | ✅ Native | ✅ (5.7+) | ⚠️ Basic |
| **Spring Data JPA** | ✅ Excellent | ⚠️ MongoTemplate | ✅ | ✅ Test only |
| **Relational Integrity** | ✅ FK constraints | ❌ Manual enforcement | ✅ | ✅ |
| **DevOps Maturity** | ✅ RDS, Cloud SQL | ✅ Atlas, Cosmos | ✅ | ❌ Single node |
| **Docker Image Size** | ⚠️ ~300MB | ✅ ~700MB | ⚠️ ~500MB | ✅ Minimal |

### Architecture

```
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│   User API  │ ────────▶ │ Spring Data │ ────────▶ │  PostgreSQL │
│   (Spring)  │  JPA/Hibernate         │   (Port 5432)│
│             │         │             │         │             │
└─────────────┘         └─────────────┘         │  ┌─────────┐│
                                                  │  │ users   ││
                                                  │  │ table   ││
                                                  │  └─────────┘│
                                                  └─────────────┘
```

### Schema Design

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- For future audit logging
CREATE TABLE user_audit (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    action VARCHAR(20) NOT NULL,  -- CREATE, UPDATE, DELETE
    changed_fields JSONB,
    performed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Consequences

### Positive
- **ACID compliance**: Guaranteed consistency for user mutations
- **Complex queries**: Efficient pagination, sorting, full-text search
- **Future-proof**: Easy to add relations (preferences, roles, audit logs)
- **Industry standard**: DevOps teams familiar with PostgreSQL
- **JSON support**: Can store flexible metadata in JSONB columns
- **Hibernate/JPA**: Mature ORM with caching, lazy loading, validation

### Negative
- **Schema migrations**: Need Flyway/Liquibase for schema changes
- **Horizontal scaling**: Harder to shard than MongoDB (not needed at current scale)
- **Document flexibility**: Less flexible than MongoDB for unstructured data
- **Learning curve**: SQL knowledge required (vs MongoDB's simpler queries)

### Mitigations
- Use **Flyway** for database migrations
- JSONB columns for flexible metadata (hybrid approach)
- Read replicas if read scaling needed (future)

## Alternatives Considered

### Option A: MongoDB
- Document store with flexible schema
- Horizontal scaling via sharding
- **Rejected**: 
  - Current data model is highly relational (users → audit logs → future preferences)
  - ACID transactions across collections are complex
  - Aggregation pipeline for complex queries is harder than SQL
  - Spring Data MongoDB less mature than JPA

### Option B: MySQL
- Popular open-source RDBMS
- Widely supported
- **Rejected**: PostgreSQL has better JSON support, full-text search, and stricter ACID compliance. MySQL 8.0 closes the gap but PostgreSQL ecosystem is preferred.

### Option C: H2 (Production)
- Embedded, zero-config
- **Rejected**: Not suitable for production; no concurrent write support, data loss risk on crashes

## References
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Spring Data JPA Guide](https://docs.spring.io/spring-data/jpa/reference/)
- [PostgreSQL vs MongoDB Comparison](https://www.postgresql.org/about/featurematrix/)

## Date
2026-04-15

## Author
API Development Team
