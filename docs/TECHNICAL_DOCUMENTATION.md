# Technical Documentation - Palmonas CRM Admin Portal

## 1. System Design

### 1.1 Architecture Overview

The application follows a **modular monolith** architecture deployed as a single Spring Boot application with clear domain boundaries between modules. This approach provides the organizational benefits of microservices while avoiding distributed system complexity in an MVP.

```
┌─────────────────────────────────────────────────────────┐
│                    Nginx Reverse Proxy                   │
│                      (Port 80)                          │
└──────────────┬──────────────────────┬───────────────────┘
               │                      │
    ┌──────────▼──────────┐  ┌───────▼────────┐
    │   React Frontend    │  │  Spring Boot   │
    │   (Port 3000)       │  │  Backend       │
    │                     │  │  (Port 8080)   │
    │  - Dashboard        │  │                │
    │  - Orders           │  │  Modules:      │
    │  - Login            │  │  - Auth/JWT    │
    │  - Settings         │  │  - Orders      │
    └─────────────────────┘  │  - Channels    │
                             │  - Notification│
                             │                │
                             │  Infrastructure│
                             │  - Resilience  │
                             │  - Logging     │
                             │  - Metrics     │
                             │  - Lifecycle   │
                             └───┬────────┬───┘
                                 │        │
                        ┌────────▼──┐  ┌──▼─────────┐
                        │PostgreSQL │  │   Redis     │
                        │  (5432)   │  │   (6379)   │
                        └───────────┘  └────────────┘
```

### 1.2 Module Responsibilities

| Module | Responsibility |
|--------|---------------|
| `auth` | JWT token generation, login/register, token refresh/revocation |
| `user` | User entity, RBAC role definitions |
| `order` | Order CRUD, filtering, pagination, status state machine, dashboard stats |
| `channel` | Channel adapters (Amazon, Flipkart, Website), scheduled sync, simulators |
| `notification` | Dead letter queue for failed operations |
| `infrastructure/resilience` | Circuit breaker, retry, rate limiter configuration |
| `infrastructure/logging` | Correlation ID filter, structured JSON logging |
| `infrastructure/metrics` | Custom health indicators, Prometheus metrics |
| `infrastructure/lifecycle` | Startup validation, graceful shutdown, admin seeder |

### 1.3 Database Schema

The database uses PostgreSQL 16 with UUID primary keys and proper indexing:

**Tables:**
- `users` - Admin portal users with role-based access
- `refresh_tokens` - JWT refresh token storage with revocation support
- `orders` - Multi-channel orders with JSONB metadata
- `order_items` - Line items for each order
- `order_status_history` - Audit trail of status changes
- `dead_letter_queue` - Failed operations for retry
- `feature_flags` - Runtime feature toggle configuration

**Key Indexes:**
- `orders(channel)`, `orders(status)`, `orders(ordered_at)` - individual filters
- `orders(channel, status)` - composite filter for common query pattern
- `orders(external_order_id)` - unique constraint, used for deduplication
- `orders(created_at DESC)` - default sort order

### 1.4 API Contract

Full API documentation is auto-generated at `/api/docs/swagger-ui.html` using SpringDoc OpenAPI 3.

**Key API Patterns:**
- All responses wrapped in `ApiResponse<T>` with `success`, `message`, `data`, `errors`, `timestamp`
- Paginated responses use `PagedResponse<T>` with `content`, `page`, `size`, `totalElements`, `totalPages`
- Authentication via `Authorization: Bearer <token>` header
- Correlation IDs in `X-Correlation-Id` response header

### 1.5 Data Flow - Order Status Update

```
Client Request (PUT /orders/{id}/status)
  → JWT Authentication Filter (validate token, extract userId)
  → RBAC Check (@PreAuthorize ADMIN+ role)
  → OrderController.updateStatus()
  → OrderService.updateStatus()
    → Load Order from DB
    → Validate status transition (state machine)
    → Create OrderStatusHistory entry (audit trail)
    → Update Order status
    → Save to PostgreSQL
    → Return updated OrderResponse
  → ApiResponse wrapping
  → Correlation ID in response header
```

### 1.6 Channel Integration Architecture

```
ChannelSyncService (Scheduled every 5 min)
  │
  ├── AmazonAdapter
  │   ├── @CircuitBreaker(name="amazonChannel")
  │   ├── @Retry(name="amazonChannel", 3 attempts, exp backoff)
  │   ├── @Bulkhead(name="amazonChannel", max 10 concurrent)
  │   └── Fallback → return empty list + log warning
  │
  ├── FlipkartAdapter (same resilience pattern)
  │
  └── WebsiteAdapter (same resilience pattern)
  
  Each adapter:
  1. Simulates external API call with configurable latency
  2. On success: maps ChannelOrder → Order entity, saves to DB
  3. On failure: records to Dead Letter Queue
  4. Deduplication via external_order_id unique constraint
```

---

## 2. Resilience & Reliability

### 2.1 Error Handling Strategy

**Layered error handling:**
1. **Controller layer**: `@Valid` input validation → `MethodArgumentNotValidException`
2. **Service layer**: Domain-specific exceptions (`BadRequestException`, `ResourceNotFoundException`)
3. **Infrastructure layer**: `GlobalExceptionHandler` catches all exceptions, returns consistent `ApiResponse`
4. **External calls**: Resilience4j circuit breaker + retry + fallback

### 2.2 Circuit Breaker Configuration

| Parameter | Value | Rationale |
|-----------|-------|-----------|
| Sliding window size | 10 | Small enough to react quickly |
| Failure rate threshold | 50% | Opens after 5/10 failures |
| Wait in open state | 30s | Allow external service recovery |
| Permitted calls in half-open | 3 | Cautious recovery probing |
| Slow call threshold | 80% | Detect degraded performance |
| Slow call duration | 3s | API should respond within 3s |

### 2.3 Retry Configuration

- Max attempts: 3
- Backoff: Exponential (1s, 2s, 4s)
- Retried exceptions: IOException, TimeoutException

### 2.4 Failover Scenarios

| Scenario | Behavior |
|----------|----------|
| Channel API down | Circuit breaker opens → fallback returns empty list → warning logged |
| Redis down | App continues with degraded caching → startup warns but doesn't fail |
| Database down | Startup fails fast → health check returns DOWN |
| High latency | Bulkhead limits concurrent calls → slow call detection triggers circuit breaker |

### 2.5 Dead Letter Queue

Failed channel sync operations are persisted to the `dead_letter_queue` table with:
- Operation type and payload
- Error message and stack trace
- Retry count (max 5)
- Status progression: PENDING → RETRIED → FAILED/RESOLVED

Admin can view and manually resolve entries via `/api/admin/dead-letters`.

---

## 3. Performance & Scalability

### 3.1 Performance Targets

| Metric | Target | Implementation |
|--------|--------|---------------|
| API p95 latency | < 500ms | Connection pooling, indexed queries, caching |
| Concurrent users | 100+ | HikariCP pool (20 connections), stateless JWT |
| DB query time | < 100ms | Composite indexes, specification-based filtering |
| Startup time | < 30s | Flyway migrations, preflight checks |

### 3.2 Caching Strategy

| Cache | TTL | Purpose |
|-------|-----|---------|
| `orders` | 5 min | Cached order listings |
| `featureFlags` | 1 min | Hot-reloadable feature flags |
| `channelOrders` | 2 min | Cached channel sync results |

Cache invalidation: Explicit `@CacheEvict` on write operations.

### 3.3 Database Optimization

- **Connection pooling**: HikariCP with 20 max connections, 5 minimum idle
- **Batch operations**: Hibernate batch size 25 for inserts/updates
- **Lazy loading**: All relationships `FetchType.LAZY` to avoid N+1
- **Pagination**: All list endpoints paginated (max 100 per page)
- **Specification queries**: Dynamic filtering without string concatenation

### 3.4 Load Testing

Three k6 test scenarios are included:

1. **Normal Load** (`normal-load.js`): 50 VUs for 5 minutes
   - Threshold: p95 < 500ms, error rate < 5%
   
2. **Peak Load** (`peak-load.js`): Ramp to 150 VUs over 7 minutes
   - Threshold: p95 < 1000ms, error rate < 10%

3. **Stress Test** (`stress-test.js`): Ramp to 400 VUs to find breaking point
   - Mixed workload: 40% list, 30% stats, 20% search, 10% health

### 3.5 Scaling Strategy

**Horizontal scaling:**
- Stateless backend (JWT, no server sessions) → multiple instances behind load balancer
- Redis for shared cache/rate limiting across instances
- PostgreSQL read replicas for read-heavy workloads

**Vertical scaling:**
- JVM tuning: `-XX:MaxRAMPercentage=75.0` respects container memory limits
- HikariCP pool sizing based on available connections

---

## 4. Security

### 4.1 Authentication Flow

```
1. Login: POST /auth/login {email, password}
   → BCrypt password verification
   → Generate JWT access token (15 min, HS512)
   → Generate refresh token (UUID, stored in DB, 7 day expiry)
   → Return both tokens + user info

2. API Request: GET /orders (Authorization: Bearer <access_token>)
   → JwtAuthenticationFilter extracts token
   → Validate signature + expiry
   → Extract userId + role → set SecurityContext
   → @PreAuthorize checks role

3. Token Refresh: POST /auth/refresh {refreshToken}
   → Validate refresh token exists, not revoked, not expired
   → Revoke old refresh token (rotation)
   → Generate new access + refresh tokens

4. Logout: POST /auth/logout {refreshToken}
   → Revoke refresh token in DB
```

### 4.2 RBAC Model

| Role | Permissions |
|------|------------|
| SUPER_ADMIN | All operations + user management + feature flags + dead letters |
| ADMIN | Order CRUD + status updates |
| VIEWER | Read-only access to orders and dashboard |

### 4.3 Security Measures

| Category | Measure |
|----------|---------|
| Input | Jakarta Bean Validation (`@Valid`, `@Pattern`, `@Size`) on all DTOs |
| SQL Injection | Spring Data JPA parameterized queries throughout |
| XSS | Content-Security-Policy header, React's default escaping |
| Headers | HSTS, X-Frame-Options, X-Content-Type-Options, Referrer-Policy |
| CORS | Whitelist only frontend origins |
| Rate Limiting | Resilience4j RateLimiter (100 req/s default) |
| Secrets | `.env` file (gitignored), `.env.example` with placeholders |
| Containers | Non-root user, multi-stage builds, minimal base images |

### 4.4 Secrets Management

- All secrets via environment variables (never in code or Docker images)
- `.env` file gitignored, `.env.example` provides template
- JWT secrets minimum 32 characters, validated at startup
- Database passwords, Redis passwords all externalized

---

## 5. Development & Deployment

### 5.1 Local Setup

1. Install prerequisites: Docker, Docker Compose
2. Clone repository
3. Copy `.env.example` to `.env`
4. Run `docker compose up --build`
5. Access frontend at `http://localhost:3000`
6. Login with `admin@palmonas.com` / `Admin@123`

### 5.2 Docker Services

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| postgres | postgres:16-alpine | 5432 | Primary database |
| redis | redis:7-alpine | 6379 | Cache + rate limiting |
| backend | Custom (Eclipse Temurin 21) | 8080 | Spring Boot API |
| frontend | Custom (Nginx Alpine) | 3000 | React SPA |
| nginx | nginx:alpine | 80 | Reverse proxy |

### 5.3 Environment Profiles

| Profile | Purpose |
|---------|---------|
| `dev` | Verbose SQL logging, formatted output |
| `prod` | Minimal logging, larger connection pools, JSON structured logs |

### 5.4 Database Migrations

Flyway runs automatically on startup:
- `V1` - Users table
- `V2` - Refresh tokens
- `V3` - Orders, items, status history (with indexes)
- `V4` - Dead letter queue
- `V5` - Feature flags with defaults
- `V6` - Seed admin user
- `V7` - Seed 60 sample orders across 3 channels

---

## 6. Assumptions & Trade-offs

### 6.1 Assumptions

- Channel APIs are simulated since real Amazon/Flipkart seller APIs require OAuth credentials
- Single-currency (INR) for all orders in the MVP
- Admin user seeded on startup; self-registration creates VIEWER role only
- In-memory channel simulation rather than separate microservices

### 6.2 Technology Choices

| Choice | Rationale |
|--------|-----------|
| Modular monolith over microservices | Simpler deployment for MVP, still well-organized modules |
| PostgreSQL over MongoDB | ACID compliance needed for order management, JSONB for flexible metadata |
| JWT over sessions | Stateless, horizontally scalable, no session store needed |
| Resilience4j over Hystrix | Modern, maintained, native Spring Boot 3 support |
| k6 over JMeter | Scriptable in JavaScript, lightweight, better DX |
| shadcn/ui over Material UI | Tree-shakeable, Tailwind-native, copy-paste approach gives full control |

### 6.3 Known Limitations

- Channel simulators don't persist state between syncs
- No real-time WebSocket updates (polling-based refresh)
- No email notification system
- No bulk order operations in the UI
- Rate limiting is per-instance (not distributed without Redis Lua scripts)

### 6.4 Future Improvements

- WebSocket for real-time order updates
- Elasticsearch for full-text search at scale
- Kubernetes deployment with HPA
- Distributed tracing with Jaeger/Zipkin
- CI/CD pipeline (GitHub Actions)
- E2E tests with Playwright

---

## 7. AI Tools Usage

### 7.1 Tools Used

- **Claude (Anthropic)**: Primary AI assistant for architecture design, code generation, and documentation
- **Cursor IDE**: AI-powered IDE for code completion and editing

### 7.2 Use Cases

| Task | AI Contribution |
|------|----------------|
| Architecture planning | High-level system design, technology selection rationale |
| Code generation | Entity models, repository interfaces, controller scaffolding |
| Configuration | Spring Boot YAML config, Docker Compose, Resilience4j settings |
| Seed data | Realistic Indian e-commerce order data generation |
| Documentation | Technical documentation structure and content |
| Testing | Unit test templates, k6 load test scripts |

### 7.3 Productivity Impact

AI tools significantly accelerated development by:
- Reducing boilerplate code writing time by approximately 60%
- Providing consistent patterns across modules
- Generating realistic seed data that would be tedious to create manually
- Ensuring comprehensive documentation coverage

### 7.4 Human Decisions

All architectural decisions, technology choices, security configurations, and business logic validation rules were human-directed. AI assisted with implementation but the design was fully specified by the developer.
