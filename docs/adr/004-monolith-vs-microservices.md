# ADR 004: Monolith Architecture (for MVP)

## Status
Accepted

## Context
Starting a new user management API with unknown scale and feature scope. Need to decide architectural approach for initial development and future evolution.

### Current Context
- Single development team (1-3 developers)
- MVP scope: CRUD operations, authentication, rate limiting
- Unknown user scale (target: 1K-100K users initially)
- Limited DevOps resources (no dedicated platform team)
- Rapid iteration required for product-market fit

### Requirements
- Fast development velocity
- Simple deployment (single artifact)
- Easy local development
- Clear upgrade path if scaling needed
- Cost-effective infrastructure

## Decision
Start with **Monolith** architecture, with clear boundaries for future extraction.

### Why Monolith (for MVP)

| Criteria | Monolith (Modular) | Microservices | Modular Monolith |
|----------|-------------------|---------------|------------------|
| **Dev Velocity** | ✅ Fast, single codebase | ❌ Slow (API contracts, deployments) | ✅ Fast with modules |
| **Debugging** | ✅ Simple stack traces | ❌ Distributed tracing needed | ✅ Module-level debugging |
| **Deployment** | ✅ Single container | ❌ Complex orchestration | ✅ Single deploy |
| **Team Size** | ✅ 1-3 developers | ❌ Need multiple teams | ✅ 1-3 developers |
| **Resource Cost** | ✅ 1 container | ❌ N containers | ✅ 1 container |
| **Scalability** | ⚠️ Scale whole app | ✅ Scale per service | ⚠️ Scale whole app |
| **Technology Mix** | ⚠️ Same stack | ✅ Polyglot | ⚠️ Same stack |

### Architecture

**Current: Modular Monolith**
```
┌─────────────────────────────────────────────────────────┐
│                    User API (Single JAR)                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │
│  │  Controller │  │   Service   │  │  Repository     │  │
│  │   Layer     │──│   Layer     │──│   (JPA)         │  │
│  └─────────────┘  └─────────────┘  └─────────────────┘  │
│         │                  │                  │          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │
│  │ Rate Limit  │  │   Security  │  │   Config        │  │
│  │   (AOP)     │  │  (Keycloak) │  │   (Profiles)    │  │
│  └─────────────┘  └─────────────┘  └─────────────────┘  │
└─────────────────────────────────────────────────────────┘
                           │
                    ┌──────────────┐
                    │  PostgreSQL  │
                    └──────────────┘
```

**Future: Decomposed (if needed)**
```
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│   User Service   │  │   Auth Service   │  │  Audit Service   │
│   (extracted)    │  │   (Keycloak)     │  │   (new)          │
└────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘
         │                     │                     │
         └─────────────────────┼─────────────────────┘
                               │
                    ┌───────────▼───────────┐
                    │     PostgreSQL        │
                    │  (per-service schema) │
                    └───────────────────────┘
```

### Module Boundaries (for Future Extraction)

```
com.fugitivalamadrid.api.userapi/
├── user/                    # User domain (future: User Service)
│   ├── controller/
│   ├── service/
│   ├── repository/
│   └── model/
├── auth/                    # Authentication (future: Auth Service)
│   └── config/
├── ratelimit/               # Rate limiting (shared utility)
├── config/                  # Shared configuration
└── exception/               # Shared exception handling
```

## Consequences

### Positive
- **Fast development**: No network calls between services, shared database transactions
- **Simple operations**: Single deploy, single monitoring target, single log stream
- **Refactoring ease**: IDE refactoring across entire codebase
- **Testing**: Integration tests run in-process, fast and reliable
- **Cost**: Single container on single VM/ECS task

### Negative
- **Tight coupling risk**: Modules may become entangled if boundaries not respected
- **Scale limitations**: Can't scale user-reads independently of writes
- **Technology lock-in**: All services must use Java/Spring Boot
- **Deployment coordination**: Any change requires full redeploy

### Mitigations
- **Package-private visibility**: Encapsulate module internals
- **Clean interfaces**: Service layer interfaces define module contracts
- **No shared tables**: Each module owns its schema (prep for extraction)
- **Feature flags**: Enable gradual rollouts within monolith

## Decomposition Triggers (When to Split)

Consider microservices when:
- ✅ Team grows to 8+ developers (Conway's Law)
- ✅ One module scales 10x differently than others
- ✅ Different modules need different tech stacks (e.g., ML service in Python)
- ✅ Deployment frequency varies significantly (e.g., auth rarely changes, users daily)
- ✅ Isolation requirements (security, compliance)

## Alternatives Considered

### Option A: Microservices from Day 1
- Separate deployables for user-service, auth-service, audit-service
- **Rejected**: 
  - Premature optimization for unknown scale
  - Adds network latency, distributed transaction complexity
  - Requires Kubernetes expertise we don't have
  - Slower development due to API versioning

### Option B: Serverless (AWS Lambda)
- Function-per-endpoint deployment
- **Rejected**:
  - Cold start latency unacceptable for user-facing API
  - Complex local development
  - Vendor lock-in to AWS
  - Harder to implement rate limiting across functions

## Migration Path (Future)

If decomposition needed:
1. **Phase 1**: Extract database schemas per module
2. **Phase 2**: Add API layer (REST/gRPC) behind interfaces
3. **Phase 3**: Move to separate processes with shared-nothing
4. **Phase 4**: Independent deployments with feature flags

## References
- [Monolith First](https://martinfowler.com/bliki/MonolithFirst.html) by Martin Fowler
- [Modular Monolith](https://shopify.engineering/building-and-scaling-shopify-into-modular-monolith) by Shopify
- [Conway's Law](https://en.wikipedia.org/wiki/Conway%27s_law)

## Date
2026-04-15

## Author
API Development Team
