# ADR 005: Spring Boot over Node.js/Go

## Status
Accepted

## Context
Selecting the primary technology stack for building a production-ready REST API. Team has mixed experience with Java, JavaScript, and Go.

### Requirements
- Robust ecosystem for enterprise patterns (security, ORM, validation)
- Mature testing frameworks (unit, integration, e2e)
- Observable (metrics, tracing, logging)
- Container-friendly (JVM tuning, startup time)
- Team expertise and hiring pool
- Long-term maintenance (5+ years)

## Decision
Use **Spring Boot (Java 17)** as the primary framework.

### Why Spring Boot

| Criteria | Spring Boot | Node.js + Express/NestJS | Go + Gin/Fiber | Python + FastAPI |
|----------|-------------|-------------------------|----------------|------------------|
| **Ecosystem Maturity** | ✅ 20+ years, enterprise-grade | ⚠️ Good but fragmented | ⚠️ Growing | ✅ Good for APIs |
| **ORM/Data Access** | ✅ Spring Data JPA (excellent) | ⚠️ Prisma/TypeORM (good) | ⚠️ GORM/Ent (adequate) | ✅ SQLAlchemy |
| **Security** | ✅ Spring Security, OAuth2 | ⚠️ Passport.js (manual) | ⚠️ Libraries exist | ⚠️ FastAPI has auth |
| **Observability** | ✅ Actuator, Micrometer, Sleuth | ⚠️ Libraries needed | ⚠️ Prometheus client | ⚠️ Manual setup |
| **Testing** | ✅ JUnit 5, Testcontainers, MockMvc | ✅ Jest, Supertest | ⚠️ Standard library | ✅ Pytest |
| **Async/Concurrency** | ✅ Virtual Threads (Project Loom) | ✅ Event loop | ✅ Goroutines | ⚠️ GIL issues |
| **Startup Time** | ⚠️ 3-5 seconds | ✅ <1 second | ✅ <100ms | ✅ 1-2 seconds |
| **Memory Footprint** | ⚠️ 200-400MB | ✅ 50-100MB | ✅ 10-20MB | ⚠️ 100-200MB |
| **Hiring Pool** | ✅ Large, enterprise-focused | ✅ Very large | ⚠️ Smaller | ✅ Large |
| **Type Safety** | ✅ Strong (Java) | ⚠️ TypeScript optional | ✅ Strong | ⚠️ Dynamic |

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                   │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐  │
│  │                   Spring Ecosystem                     │  │
│  │  ┌─────────────┐ ┌─────────────┐ ┌───────────────┐ │  │
│  │  │   Spring    │ │   Spring    │ │   Spring      │ │  │
│  │  │   Web MVC   │ │   Security  │ │   Data JPA    │ │  │
│  │  └─────────────┘ └─────────────┘ └───────────────┘ │  │
│  │  ┌─────────────┐ ┌─────────────┐ ┌───────────────┐ │  │
│  │  │   Spring    │ │   Spring    │ │   Spring Boot │ │  │
│  │  │   Validation│ │   Cache     │ │   Actuator    │ │  │
│  │  └─────────────┘ └─────────────┘ └───────────────┘ │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐  │
│  │              JVM (Java 17 with Loom)                 │  │
│  │         Virtual Threads for high concurrency           │  │
│  └─────────────────────────────────────────────────────┘  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
                           │
           ┌───────────────┼───────────────┐
           ▼               ▼               ▼
    ┌────────────┐  ┌────────────┐  ┌────────────┐
    │ PostgreSQL │  │  Keycloak  │  │  SonarQube │
    └────────────┘  └────────────┘  └────────────┘
```

## Consequences

### Positive
- **Batteries included**: Spring Boot starters provide auto-configuration for security, data, validation
- **Production-ready**: Actuator gives health, metrics, info endpoints out-of-the-box
- **Enterprise patterns**: Transaction management, AOP, dependency injection mature and well-documented
- **Testing**: Excellent integration with Testcontainers for realistic test environments
- **Virtual Threads (Java 21)**: Handles high concurrency with simple thread-per-request model
- ** hiring**: Java developers widely available; Spring Boot is industry standard

### Negative
- **Startup time**: 3-5 seconds (mitigated by JVM tuning, CDS, AOT compilation)
- **Memory usage**: 200-400MB baseline (acceptable for modern hardware)
- **Verbosity**: More boilerplate than Go or Python (mitigated by Lombok, records)
- **Learning curve**: Spring ecosystem is vast and complex for beginners

### Mitigations
- Use **GraalVM native images** for faster startup/lower memory (if needed)
- **Project Loom** (Java 21) replaces complex reactive programming
- **Spring Boot DevTools** for fast restart in development

## Alternatives Considered

### Option A: Node.js + NestJS
- TypeScript, decorator-based (similar to Spring)
- Fast development, huge npm ecosystem
- **Rejected**:
  - npm dependency chaos (left-pad incidents)
  - Single-threaded event loop harder for CPU-intensive tasks
  - TypeScript adds complexity without JVM-level optimization
  - Less mature enterprise security patterns

### Option B: Go
- Fast, efficient, excellent concurrency
- Single binary deployment
- **Rejected**:
  - Smaller ecosystem (ORMs less mature than JPA)
  - More manual error handling
  - Team has less Go experience
  - Less suitable for complex business logic/DDD
  - Good for future high-throughput services, not current CRUD API

### Option C: Python + FastAPI
- Fast development, excellent async support
- Great for ML integration
- **Rejected**:
  - GIL limits true parallelism for CPU tasks
  - Type hints optional (runtime errors possible)
  - Less mature in enterprise deployment patterns
  - Slower than Java for high-throughput APIs

## When to Revisit This Decision

Consider alternatives when:
- **Specialized workloads**: High-throughput streaming (Go), ML inference (Python), real-time WebSockets (Node.js)
- **Greenfield services**: New service doesn't need JPA/enterprise patterns
- **Serverless requirements**: Function-as-a-Service (Go or Node.js have faster cold starts)

## References
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Java 21 Virtual Threads](https://openjdk.org/jeps/444)
- [Spring Boot vs Node.js Benchmarks](https://web-frameworks-benchmark.netlify.app/)
- [Why Java for Microservices](https://www.infoq.com/articles/java-microservices-2023/)

## Date
2026-04-15

## Author
API Development Team
