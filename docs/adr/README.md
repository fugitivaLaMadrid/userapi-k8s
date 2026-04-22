# Architectural Decision Records (ADRs)

This directory contains Architectural Decision Records for the User API project.

## What is an ADR?

An Architectural Decision Record (ADR) captures an important architectural decision made along with its context and consequences. ADRs help teams:
- Remember why decisions were made
- Onboard new developers faster
- Avoid revisiting the same discussions
- Document technical debt and trade-offs

## Index

| Number | Title | Status | Date |
|--------|-------|--------|------|
| [001](001-keycloak-authentication.md) | Authentication with Keycloak | Accepted | 2026-04-15 |
| [002](002-bucket4j-rate-limiting.md) | Rate Limiting with Bucket4j | Accepted | 2026-04-15 |
| [003](003-postgresql-vs-mongodb.md) | PostgreSQL vs MongoDB | Accepted | 2026-04-15 |
| [004](004-monolith-vs-microservices.md) | Monolith vs Microservices | Accepted | 2026-04-15 |
| [005](005-spring-boot-over-nodejs-go.md) | Spring Boot over Node.js/Go | Accepted | 2026-04-15 |

## Status Definitions

- **Proposed**: Under discussion, not yet decided
- **Accepted**: Decision approved and implemented
- **Deprecated**: Decision no longer relevant, but kept for history
- **Superseded**: Replaced by a newer ADR (link to new ADR)

## Creating a New ADR

1. Copy [TEMPLATE.md](TEMPLATE.md)
2. Name it `[NUMBER]-[short-title].md`
3. Fill in all sections
4. Update this index
5. Submit for review via PR

## References

- [ADR GitHub Organization](https://adr.github.io/)
- [MADR - Markdown ADR Template](https://adr.github.io/madr/)
- [Documenting Architecture Decisions](http://thinkrelevance.com/blog/2011/11/15/documenting-architecture-decisions) (original ADR blog post)
