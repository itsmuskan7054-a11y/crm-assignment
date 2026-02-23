# Palmonas CRM - Admin Portal

A production-grade admin CRM portal for managing customer orders from multiple e-commerce platforms (Amazon, Flipkart, Organic Website). Built with Java Spring Boot (modular monolith) and React (TypeScript).

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.3, Spring Security, Spring Data JPA |
| Frontend | React 18, Vite, TypeScript, Tailwind CSS, shadcn/ui |
| Database | PostgreSQL 16 with Flyway migrations |
| Cache | Redis 7 (caching, rate limiting) |
| Auth | JWT (access + refresh tokens), BCrypt |
| Resilience | Resilience4j (circuit breaker, retry, bulkhead, rate limiter) |
| Monitoring | Micrometer, Prometheus, Spring Boot Actuator |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Load Testing | k6 |
| Containers | Docker, Docker Compose |

## Quick Start

### Prerequisites

- Docker & Docker Compose
- (Optional) Java 21, Node.js 20, Maven 3.9

### Run with Docker Compose

```bash
# Clone the repository
git clone https://github.com/itsmuskan7054-a11y/crm-assignment.git
cd crm-assignment

# Copy environment file
cp .env.example .env

# Build and start all services (first run takes ~3-5 min)
docker compose up --build

# Wait for "Started CrmApplication" in the backend logs, then open:
# Frontend + API:  http://localhost:3000
# Swagger UI:      http://localhost:8080/api/docs/swagger-ui.html
```

> **Note:** The backend takes ~30-60s to start (Flyway migrations + seed data on first run).
> The frontend at `http://localhost:3000` proxies all `/api` calls to the backend automatically.

### Default Admin Credentials

```
Email: admin@palmonas.com
Password: Admin@123
```

### Local Development (without Docker)

**Backend:**

```bash
# Start PostgreSQL and Redis (via Docker or locally)
docker compose up postgres redis -d

# Run Spring Boot
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

**Frontend:**

```bash
cd frontend
npm install
npm run dev
# Opens at http://localhost:3000
```

## Project Structure

```
palmonas-assignment/
├── backend/                    # Spring Boot application
│   ├── src/main/java/com/palmonas/crm/
│   │   ├── config/            # Security, Redis, CORS, OpenAPI configs
│   │   ├── common/            # Shared DTOs, exceptions, utilities
│   │   ├── module/
│   │   │   ├── auth/          # JWT authentication, login/register
│   │   │   ├── user/          # User entity, RBAC roles
│   │   │   ├── order/         # Order CRUD, filtering, status machine
│   │   │   ├── channel/       # Channel adapters (Amazon, Flipkart, Website)
│   │   │   └── notification/  # Dead letter queue
│   │   └── infrastructure/
│   │       ├── resilience/    # Circuit breaker configs
│   │       ├── logging/       # Correlation ID, structured logging
│   │       ├── metrics/       # Health checks, custom metrics
│   │       └── lifecycle/     # Startup validation, graceful shutdown
│   └── src/main/resources/
│       ├── db/migration/      # Flyway SQL migrations (V1-V7)
│       └── application*.yml   # Spring Boot configuration
├── frontend/                  # React + TypeScript application
│   └── src/
│       ├── components/        # Reusable UI components
│       ├── pages/             # Login, Dashboard, Orders, Settings
│       ├── services/          # Axios API client
│       ├── store/             # Auth state management
│       └── types/             # TypeScript interfaces
├── load-tests/                # k6 load testing scripts
├── docs/                      # Technical documentation
├── nginx/                     # Reverse proxy configuration
├── docker-compose.yml         # Multi-service orchestration
└── .env.example               # Environment variable template
```

## API Endpoints

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | Login with email/password |
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/refresh` | Refresh access token |
| POST | `/api/auth/logout` | Logout and revoke refresh token |

### Orders
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/orders` | List orders (paginated, filterable, sortable) |
| GET | `/api/orders/{id}` | Get order details with items and history |
| PUT | `/api/orders/{id}/status` | Update order status (ADMIN+) |
| GET | `/api/orders/stats` | Dashboard statistics |

### Admin (SUPER_ADMIN only)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/feature-flags` | List all feature flags |
| PUT | `/api/admin/feature-flags/{key}` | Toggle feature flag |
| GET | `/api/admin/dead-letters` | View dead letter entries |
| POST | `/api/admin/sync-channels` | Manually trigger channel sync |

### Health & Monitoring
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | Basic health check |
| GET | `/api/health/ready` | Readiness probe |
| GET | `/api/health/live` | Liveness probe |
| GET | `/api/actuator/prometheus` | Prometheus metrics |

## Features

### Core
- Multi-platform order aggregation from 3 channels (Amazon, Flipkart, Website)
- Full order management with filtering, sorting, search, and pagination
- Order status machine with validated transitions
- Role-based access control (SUPER_ADMIN, ADMIN, VIEWER)

### Resilience
- Circuit breaker per channel (Resilience4j)
- Retry with exponential backoff (1s, 2s, 4s)
- Bulkhead isolation (max 10 concurrent calls per channel)
- Dead letter queue for failed operations
- Graceful degradation when channels are unavailable

### Observability
- Structured JSON logging (Logback + Logstash encoder)
- Correlation ID propagation (X-Correlation-Id)
- Micrometer metrics (request count, latency, circuit breaker state)
- Health check endpoints (liveness, readiness)
- Spring Boot Actuator with Prometheus export

### Security
- JWT authentication with refresh token rotation
- BCrypt password hashing (strength 12)
- Input validation on all endpoints
- SQL injection prevention (JPA parameterized queries)
- Security headers (HSTS, X-Frame-Options, CSP)
- Rate limiting
- CORS whitelisting
- Non-root Docker containers

### Performance
- Connection pooling (HikariCP)
- Redis caching with TTL
- Database indexes on frequently queried columns
- Pagination for all list endpoints
- Server-side filtering and sorting

## Running Tests

### Backend Unit Tests

```bash
cd backend
./mvnw test
```

### Load Tests (k6)

```bash
# Install k6: https://k6.io/docs/getting-started/installation/

# Normal load (50 VUs, 5 min)
k6 run load-tests/scripts/normal-load.js

# Peak load (150 VUs, 3 min)
k6 run load-tests/scripts/peak-load.js

# Stress test (up to 400 VUs)
k6 run load-tests/scripts/stress-test.js
```

## Environment Variables

See [.env.example](.env.example) for all required configuration variables.

## License

This project is developed as a senior SDE assignment for Palmonas.
