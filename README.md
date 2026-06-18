# Intelligent Cloud Operations Platform (ICOP)

![CI](https://github.com/VineshReddyK/Intelligent-cloud-operations-platform/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-green)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-3.9.0-black)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![License](https://img.shields.io/badge/license-MIT-blue)

An **AI-powered cloud-native operations platform** built with Java 21, Spring Boot 3.5 microservices, Apache Kafka, PostgreSQL, Docker, Kubernetes, and Deep Java Library (DJL). Designed for production-grade resilience, observability, and autonomous self-healing.

---

## Architecture

```
                        ┌─────────────────┐
                        │   API Gateway   │  :8080
                        │  (Spring Cloud) │
                        └────────┬────────┘
                                 │  JWT Auth Filter
              ┌──────────────────┼──────────────────┐
              │                  │                   │
    ┌─────────▼──────┐  ┌───────▼────────┐  ┌──────▼────────┐
    │  User Service  │  │ Order Service  │  │Payment Service│
    │    :8081       │  │    :8082       │  │    :8083      │
    └─────────┬──────┘  └───────┬────────┘  └──────┬────────┘
              │                  │                   │
              └──────────────────┴───────────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │     Apache Kafka 3.9    │
                    │  order.events           │
                    │  payment.events         │
                    └────────────┬────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │  Notification Service   │
                    │         :8084           │
                    └─────────────────────────┘
              ┌──────────────────────────────────────┐
              │            PostgreSQL 16             │
              │  user_db  │  order_db  │  payment_db │
              └──────────────────────────────────────┘
```

---

## Microservices

| Service | Port | Responsibilities |
|---|---|---|
| **API Gateway** | 8080 | JWT validation, routing, rate limiting, CORS |
| **User Service** | 8081 | Registration, login, JWT issuance, Spring Security |
| **Order Service** | 8082 | Order lifecycle, Kafka producer (`ORDER_CREATED`, `ORDER_CANCELLED`) |
| **Payment Service** | 8083 | Payment processing, Resilience4j circuit breaker, Kafka consumer + producer |
| **Notification Service** | 8084 | Kafka consumer for all events, notification dispatch |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.0 |
| API Gateway | Spring Cloud Gateway 2024.0.1 |
| Security | Spring Security + JWT (JJWT 0.12.6) |
| ORM | Spring Data JPA + Hibernate |
| Database | PostgreSQL 16 |
| Messaging | Apache Kafka 3.9.0 (KRaft mode) |
| Resilience | Resilience4j 2.3.0 (Circuit Breaker) |
| API Docs | SpringDoc OpenAPI 2.8.6 / Swagger UI |
| Tracing | Micrometer Tracing + OTel Collector + Tempo 2.7 |
| Metrics | Prometheus 3.2 + Grafana 11.5 |
| Logging | Loki 3.4 + Promtail (JSON via logstash-logback-encoder) |
| AI/ML | Deep Java Library (Phase 4) |
| K8s Operator | Fabric8 + CRD (Phase 5) |
| Containerization | Docker (multi-stage builds) |
| Orchestration | Kubernetes / AWS EKS 1.32 |
| IaC | Terraform 1.10 |
| CI/CD | GitHub Actions |
| Build | Maven 3.9.16 |

---

## Kafka Event Flow

```
Order Service ──► [order.events] ──► Payment Service  ──► [payment.events] ──► Notification Service
                                                                              ◄── [order.events]
```

**Topics:**
- `order.events` — `ORDER_CREATED`, `ORDER_CANCELLED`
- `payment.events` — `PAYMENT_SUCCESS`, `PAYMENT_FAILED`

---

## Getting Started

### Prerequisites
- Java 21
- Maven 3.9+
- Docker + Docker Compose

### Run locally with Docker Compose

```bash
git clone https://github.com/VineshReddyK/Intelligent-cloud-operations-platform.git
cd Intelligent-cloud-operations-platform
docker-compose up -d
```

### Build all services

```bash
mvn clean package -DskipTests
```

### Run tests

```bash
mvn clean verify
```

---

## Observability

| Tool | URL | Purpose |
|---|---|---|
| **Grafana** | http://localhost:3000 | Dashboards (admin / icop_grafana) |
| **Prometheus** | http://localhost:9090 | Metrics query and targets |
| **Loki** | http://localhost:3100 | Log aggregation API |
| **Tempo** | http://localhost:3200 | Distributed trace storage |
| **OTel Collector** | localhost:4318 (HTTP) / 4317 (gRPC) | Trace ingestion |

**Trace flow:** Spring Boot → OTel Collector → Tempo → Grafana  
**Log flow:** Spring Boot JSON → Docker → Promtail → Loki → Grafana  
**Metrics flow:** Spring Boot `/actuator/prometheus` → Prometheus → Grafana

Every log line carries `traceId` and `spanId` in JSON — click a Grafana log line to jump directly to its trace in Tempo.

---

## API Documentation

Each service exposes Swagger UI at `/swagger-ui.html`:

| Service | Swagger UI |
|---|---|
| User Service | http://localhost:8081/swagger-ui.html |
| Order Service | http://localhost:8082/swagger-ui.html |
| Payment Service | http://localhost:8083/swagger-ui.html |

### Key Endpoints

**Auth**
```
POST /api/auth/register   — Register new user
POST /api/auth/login      — Login, receive JWT
```

**Orders**
```
POST   /api/orders              — Create order
GET    /api/orders/{id}         — Get order
GET    /api/orders/user/{id}    — Get user's orders
PUT    /api/orders/{id}/cancel  — Cancel order
```

**Payments**
```
POST /api/payments              — Process payment
GET  /api/payments/{id}         — Get payment
GET  /api/payments/order/{id}   — Get payment by order
```

---

## Project Phases

| Phase | Status | Description |
|---|---|---|
| **Phase 1** | ✅ Complete | Java Spring Boot microservices + Kafka + PostgreSQL |
| **Phase 2** | ✅ Complete | AWS EKS + Terraform + Helm + GitHub Actions CD |
| **Phase 3** | ✅ Complete | Prometheus + Grafana + Loki + Tempo + OTel tracing |
| **Phase 4** | 🔜 Next | AI anomaly detection + failure prediction (DJL) |
| **Phase 5** | 🔜 | Kubernetes Operator + AI-driven auto-remediation |

---

## Author

**Vinesh Reddy Kankanalapally**
