# ADR 001: Authentication with Keycloak

## Status
Accepted

## Context
The User API requires authentication and authorization for:
- Protecting user data endpoints
- Role-based access control (RBAC)
- OAuth2/OIDC compliance for future integrations
- Token-based authentication (JWT)

### Requirements
- Stateless authentication (no server-side sessions)
- Industry-standard protocols (OAuth2, OIDC)
- Integration with external identity providers in the future
- Fine-grained permissions (admin vs user roles)

## Decision
Use **Keycloak** as the Identity and Access Management (IAM) solution.

### Why Keycloak

| Criteria | Keycloak | Spring Security (standalone) | Auth0 | Cognito |
|----------|----------|------------------------------|-------|---------|
| **Self-hosted** | ✅ Free, Docker-based | ✅ Built-in | ❌ SaaS | ❌ SaaS |
| **OAuth2/OIDC** | ✅ Full support | ⚠️ Manual config | ✅ Native | ✅ Native |
| **User Management UI** | ✅ Built-in admin console | ❌ Custom build | ✅ Dashboard | ✅ Console |
| **Role/Realm Management** | ✅ Advanced | ⚠️ Custom DB | ✅ Good | ✅ Good |
| **Integration Effort** | Low | High | Very Low | Low |
| **Cost** | Free | Free | $$$ | $$ |
| **Vendor Lock-in** | None | None | High (Auth0) | Medium (AWS) |

### Architecture

```
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│   Client    │ ────────▶ │  User API   │ ────────▶ │  Keycloak   │
│  (curl/UI)  │  Bearer │   (Spring)  │ Validate  │  (Port 8180)│
│             │  Token  │             │  JWT      │             │
└─────────────┘         └─────────────┘         └─────────────┘
                              │
                              │ JWT Claims
                              ▼
                        ┌─────────────┐
                        │  PostgreSQL │
                        │   (Users)   │
                        └─────────────┘
```

## Consequences

### Positive
- **Standards compliance**: OAuth2/OIDC out of the box
- **Admin console**: Built-in user/role/realm management
- **Future-proof**: Easy to add SSO, social login, MFA
- **Decoupled**: Auth logic separate from application code
- **No custom auth code**: Less security risk

### Negative
- **Infrastructure overhead**: Extra Docker container (~500MB RAM)
- **Learning curve**: Team must learn Keycloak concepts (realms, clients, roles)
- **Startup time**: Keycloak takes 15-30s to start
- **Complexity**: Overkill for simple single-app auth

### Mitigations
- Automated realm setup via `.scripts/setup-keycloak.ps1`
- Documented admin URLs in README.md
- Local development uses pre-configured realm import

## Alternatives Considered

### Option A: Spring Security (Standalone)
- JWT generation/validation in-app
- User credentials stored in PostgreSQL
- Roles in database
- **Rejected**: Requires writing/maintaining auth code (security risk), no admin UI

### Option B: Auth0
- SaaS identity provider
- Very fast setup
- **Rejected**: Vendor lock-in, ongoing costs, data privacy concerns

### Option C: AWS Cognito
- Managed service, scales automatically
- **Rejected**: AWS dependency, harder to run locally for development

## References
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
- [OAuth2 RFC 6749](https://tools.ietf.org/html/rfc6749)

## Date
2026-04-15

## Author
API Development Team
