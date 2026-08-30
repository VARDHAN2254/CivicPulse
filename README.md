# CivicPulse

Community Event & Engagement Platform

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot 3.3.2](https://img.shields.io/badge/Spring%20Boot-3.3.2-brightgreen.svg?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Next.js 14](https://img.shields.io/badge/Next.js-14.2.35-black.svg?style=flat-square&logo=next.js)](https://nextjs.org/)
[![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16-blue.svg?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-KRaft-red.svg?style=flat-square&logo=apachekafka)](https://kafka.apache.org/)
[![Redis 7](https://img.shields.io/badge/Redis-7-red.svg?style=flat-square&logo=redis)](https://redis.io/)
[![Release: Portfolio v1.0.0](https://img.shields.io/badge/Release-Portfolio%20v1.0.0-blue.svg?style=flat-square)](#)
[![License: All Rights Reserved](https://img.shields.io/badge/License-All%20Rights%20Reserved-lightgrey.svg?style=flat-square)](LICENSE)

---

## Overview

**CivicPulse** is a distributed, high-concurrency event management and community engagement platform engineered for universities, non-profits, volunteer groups, and civic organizations. The system provides multi-tenant organization workspaces, concurrency-safe ticket reservations, automated FIFO waitlists, cryptographic rolling QR attendance validation, real-time STOMP WebSocket notifications, distributed Kafka event streaming, and end-to-end observability.

This project is published as an engineering portfolio showcase (**Portfolio Release — v1.0.0**).

---

## Problem Statement

Traditional community event management systems frequently suffer from critical architectural bottlenecks:
1. **Overselling Race Conditions**: High-demand ticket drops trigger concurrent database writes, resulting in inventory overselling without strict row-level isolation.
2. **Static Ticket Fraud**: Static QR codes and barcoded PDF passes are prone to screenshot sharing, replay attacks, and duplicate admissions.
3. **Inefficient Waitlist Processing**: Manual or batch-based waitlist reclamation leads to unused venue capacity and poor attendee experience.
4. **Lack of Real-Time Coordination**: Organizers lack instant broadcast communication channels for venue alerts, agenda shifts, or emergency announcements.
5. **Multi-Tenant Boundary Leakage**: Organizations require strict permission isolation for event editing, member management, and attendance terminals.

---

## Features

- **Multi-Tenant Organizations**: Workspace segmentation with role-based member permissions (`OWNER`, `ORGANIZER`, `MEMBER`).
- **Pessimistic Concurrency Engine**: Database row-level write locks (`SELECT ... FOR UPDATE`) preventing ticket overselling under high-throughput registration surges.
- **Automated FIFO Waitlist**: Automatic inventory reclamation and instant candidate promotion upon ticket cancellations.
- **Dynamic HMAC Rolling QR Passes**: Time-expiring HMAC-SHA256 attendance tokens (120-second validity window) preventing static ticket duplication.
- **Mobile Organizer Scanner Terminal**: Real-time camera-based check-in verification with instant double-admission defense.
- **Distributed Event Streaming**: Partitioned Apache Kafka streams across 7 dedicated domain topics for asynchronous decoupled processing.
- **Real-Time Push & Broadcast**: STOMP WebSockets for user notifications and live organizer broadcast channels.
- **Faceted Trigram Search**: PostgreSQL `pg_trgm` full-text search with faceted filtering by format, category, date, and location.
- **Community Discussion Boards**: Threaded discussion boards with organizer pinning, nested replies, and automated toxicity filters.
- **Zero-Trust Account Security**: Secure 8-digit OTP lifecycle, timing-attack-resistant recovery flows, TOTP MFA, and Redis rate limiting.

---

## Architecture

```
                                  ┌────────────────────────────────────────────────────────┐
                                  │                  Next.js 14 Frontend                   │
                                  │       App Router • TypeScript • Tailwind CSS           │
                                  └──────────────────────────┬─────────────────────────────┘
                                                             │
                                                   REST API / STOMP WebSockets
                                                             │
                                                             ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                       CivicPulse Backend (Spring Boot 3.3.2)                                     │
├──────────────────┬──────────────────┬──────────────────┬──────────────────┬───────────────────┬──────────────────┤
│  Authentication  │  Organizations   │  Event Lifecycle │   Registration   │   QR Attendance   │    Community     │
│  & Security      │  & Multi-Tenancy │  State Machine   │  & FIFO Waitlist │   Verification    │   Discussions    │
├──────────────────┼──────────────────┼──────────────────┼──────────────────┼───────────────────┼──────────────────┤
│  MDC Tracing     │  Token Bucket    │  Real-Time STOMP │  Full-Text FTS   │  Micrometer       │  Actuator        │
│  Correlation     │  Rate Limiting   │  Notifications   │  Trigram Search  │  Metrics Engine   │  Health Probes   │
└────────┬─────────┴────────┬─────────┴────────┬─────────┴────────┬─────────┴─────────┬─────────┴────────┬─────────┘
         │                  │                  │                  │                   │                  │
         ▼                  ▼                  ▼                  ▼                   ▼                  ▼
┌──────────────────┐┌──────────────────┐┌──────────────────┐┌───────────────────┐┌──────────────────┐┌────────────────┐
│  PostgreSQL 16   ││     Redis 7      ││  Apache Kafka    ││  Prometheus /     ││  HikariCP        ││  STOMP Broker  │
│  (Relational DB) ││  (Cache & Rate) ││  (Event Streams) ││  Grafana Metrics  ││  Connection Pool ││  (Real-Time)   │
└──────────────────┘└──────────────────┘└──────────────────┘└───────────────────┘└──────────────────┘└────────────────┘
```

---

## Technology Stack

| Layer | Technologies |
| :--- | :--- |
| **Backend** | Java 21, Spring Boot 3.3.2, Spring Data JPA, Hibernate, Spring Security, Spring WebSocket (STOMP), Flyway, Micrometer, Spring Boot Actuator |
| **Database & Cache** | PostgreSQL 16 (HikariCP connection pool, `pg_trgm`), Redis 7 |
| **Message Broker** | Apache Kafka 7.6.0 (KRaft mode, Spring Kafka) |
| **Frontend** | Next.js 14.2.35 (App Router), React 18, TypeScript, Tailwind CSS, Lucide Icons, Axios, QR SVG |
| **Observability** | Prometheus 2.50, Grafana 10.3.3, MDC Trace Logging (`X-Correlation-ID`) |
| **Security** | Zero-Trust OTP Flow, TOTP MFA, JWT (HMAC-SHA256), BCrypt (12 rounds), Redis Sliding-Window Rate Limiting, Input Sanitization |
| **Testing** | JUnit 5, Mockito, AssertJ, Maven Surefire, Playwright E2E |

---

## Authentication & Security

- **Stateless JWT Sessions**: Requests authenticate via HMAC-SHA256 signed bearer tokens containing user ID, tenant context, and authorities.
- **BCrypt Password Encryption**: Sensitive credentials hashed with BCrypt (cost factor 12).
- **Role-Based Access Control (RBAC)**: Enforces role boundaries across `SUPER_ADMIN`, `ADMIN`, `MODERATOR`, `ORGANIZER`, and `MEMBER`.
- **Multi-Tenant Boundary Enforcement**: Verifies ownership and organization memberships before granting access to administrative operations.
- **TOTP / MFA Enrollment**: Multi-Factor Authentication support with encrypted backup recovery codes.
- **Distributed Rate Limiting**: Redis sliding-window token bucket algorithm protecting against brute-force attempts on authentication and registration endpoints.
- **Cross-Site Scripting (XSS) Sanitization**: Input sanitization stripping unsafe tags and scripts from user submissions.

---

## OTP & Account Recovery

- **Cryptographic Generation**: 8-digit numeric OTPs generated via `java.security.SecureRandom`.
- **Hashed Storage**: OTPs are hashed using SHA-256 before database storage; raw OTPs are never stored, logged, or returned in API responses.
- **Lifecycle Constraints**: 5-minute TTL, single-use invalidation, maximum 5 verification attempts, and 60-second resend cooldown.
- **Constant-Time Enumeration Defense**: The `/forgot-password` endpoint returns uniform generic responses regardless of account existence.
- **Purpose Binding**: OTP challenges are strictly bound to intended operations (`REGISTRATION_VERIFICATION`, `PASSWORD_RESET`, `LOGIN_MFA`, `EMAIL_CHANGE`, `SENSITIVE_ACTION`).
- **Session Revocation**: Successful password recovery immediately invalidates active refresh tokens across all sessions.

---

## Email Architecture

The email subsystem is structured around a decoupled service abstraction:

```
Development:
CivicPulse Backend ──► Email Service ──► Local Development Mailbox / Testing Service (e.g. Mailpit)

Production Architecture:
CivicPulse Backend ──► Authenticated SMTP / Transactional Provider ──► User Email Inbox
```

- **Development Testing**: Outbound SMTP can be routed to a local mail testing utility (such as Mailpit on port `1025`/`8025`).
- **Production Architecture**: Configured for TLS-authenticated transactional email services (e.g., Amazon SES, SendGrid, Postmark) via standard SMTP properties.
- *Note: Production email delivery credentials are not pre-configured in this portfolio release.*

---

## Kafka/Event Processing

CivicPulse publishes immutable domain events across 7 partitioned Kafka topics:

| Topic | Partitions | Purpose |
| :--- | :--- | :--- |
| `civicpulse.events.lifecycle` | 6 | Event state transitions (`DRAFT`, `PUBLISHED`, `COMPLETED`, `CANCELLED`) |
| `civicpulse.registrations` | 12 | Ticket reservations, cancellations, and waitlist joins |
| `civicpulse.attendance` | 6 | Attendee check-in telemetry and fraud detection |
| `civicpulse.notifications` | 6 | Asynchronous push notification dispatch |
| `civicpulse.announcements` | 6 | Real-time organizer broadcasts |
| `civicpulse.moderation` | 3 | Community content moderation queue |
| `civicpulse.audit` | 6 | Security and compliance audit log |

---

## PostgreSQL

- **Schema & Migrations**: Managed via Flyway automated database migrations (`V1__init_schema.sql` through `V4__add_otp_and_security_enhancements.sql`).
- **Pessimistic Locking**: `SELECT ... FOR UPDATE` isolation locks during high-traffic checkout flows to guarantee capacity constraints.
- **Trigram Search**: Uses PostgreSQL `pg_trgm` extension for fuzzy keyword matching across event titles, organizers, tags, and cities.
- **Connection Management**: High-performance HikariCP connection pooling.

---

## Redis

- **Distributed Rate Limiting**: Sliding-window counter algorithm tracking request frequencies per IP and user ID.
- **Refresh Token Indexing**: Fast token lookup and revocation checks during session validation.
- **Ephemeral State**: Storage for OTP resend cooldown timers and temporary verification challenges.

---

## WebSockets

- **Protocol**: Spring STOMP over SockJS / native WebSocket.
- **User Notification Topic**: `/topic/notifications/{userId}` for personalized registration confirmations and waitlist alerts.
- **Event Broadcast Topic**: `/topic/events/{eventId}/announcements` for organizer live announcements.
- **Heartbeat & Reconnection**: Client-side automatic reconnection and channel subscription management.

---

## QR Attendance

- **Rolling Token Format**: Dynamic cryptographic payload structured as `{registrationId}.{timestampEpochSeconds}.{hmacSha256Signature}`.
- **Time-to-Live**: 120-second verification window with frontend auto-refresh every 60 seconds to prevent pass sharing.
- **Organizer Scanner Terminal**: Web-based camera scanner terminal with audio feedback and instant duplicate check-in rejection.

---

## Analytics

- **Platform Analytics**: Aggregate user registrations, active events, organization counts, and ticket volumes.
- **Event Analytics**: Capacity fill rates, attendance check-in percentages, waitlist conversion rates, and registration timelines.
- **Organization Metrics**: Growth metrics, event frequency, and attendee engagement scoring.

---

## Observability

- **Spring Boot Actuator**: Health probes (`/actuator/health`), liveness, readiness, and configuration inspection.
- **Prometheus Metrics**: Scrape endpoint (`/actuator/prometheus`) exposing JVM metrics, HikariCP pool stats, and custom business counters.
- **Grafana Dashboards**: Provisioned visualization dashboards for system throughput, latency percentiles, and database connections.
- **Structured MDC Logging**: Contextual correlation ID (`X-Correlation-ID`) propagation across HTTP request lifecycle and log messages.

---

## Local Development

CivicPulse can be run locally using direct backend and frontend development servers.

### Prerequisites
- **Java 21** (e.g. Eclipse Adoptium Temurin JDK 21)
- **Node.js 20+** and **npm**
- **PostgreSQL 16** (running locally or via dedicated instance on port `5432`)
- **Redis 7** (running locally or via dedicated instance on port `6379`)
- **Apache Kafka** *(Optional)*: Required if Kafka event streaming features are exercised.
- **Mail Testing Service** *(Optional)*: E.g., Mailpit or local SMTP server for development email inspection.

*(Note: Docker is NOT required to run or inspect this portfolio codebase).*

### 1. Backend Setup
```bash
# Navigate to backend directory
cd backend

# Configure environment variables (or rely on application-dev.yml defaults)
# Run backend with Maven
mvn spring-boot:run
```
The backend API will start on `http://localhost:8080`.
- Swagger UI / OpenAPI: `http://localhost:8080/swagger-ui/index.html`
- Health Endpoint: `http://localhost:8080/actuator/health`

### 2. Frontend Setup
```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```
The Next.js frontend will be accessible at `http://localhost:3000`.

---

## Environment Variables

Copy `.env.example` to `.env` to customize local settings:

| Variable | Description | Default |
| :--- | :--- | :--- |
| `APP_BASE_URL` | Frontend application URL | `http://localhost:3000` |
| `API_BASE_URL` | Backend REST API endpoint | `http://localhost:8080/api/v1` |
| `POSTGRES_DB` | PostgreSQL database name | `civicpulse_db` |
| `POSTGRES_USER` | PostgreSQL username | `civicpulse_user` |
| `POSTGRES_PASSWORD` | PostgreSQL password | *(Dev placeholder)* |
| `REDIS_HOST` | Redis server hostname | `localhost` |
| `REDIS_PORT` | Redis server port | `6379` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker address | `localhost:9092` |
| `JWT_SECRET` | 256-bit secret key for JWT signing | *(Dev placeholder)* |
| `QR_TOKEN_SECRET_KEY` | HMAC key for rolling QR token generation | *(Dev placeholder)* |

---

## Testing

### Backend Unit & Integration Tests
```bash
cd backend
mvn clean test
```
*Executes unit and integration test suites for authentication, event state machine, pessimistic registration locking, and QR verification.*

### Frontend Typecheck & Production Build
```bash
cd frontend
npx tsc --noEmit
npm run build
```
*Validates TypeScript strict types across all 26 compiled application routes.*

### Playwright End-to-End Tests
```bash
cd frontend
npx playwright test
```
*Validates core user workflows (event discovery, registration modal, QR expiration cycle, organizer scanner, and community discussions).*

---

## Known Limitations

> [!WARNING]
> **Known Limitations in this Portfolio Release:**
> 1. **External Email Delivery**: The OTP generation and verification flow is implemented, but external SMTP/email delivery to a real inbox remains unverified in the current release. Local development email testing may be used where configured.
> 2. **Development Mailbox**: Local email testing depends on an external development mailbox/SMTP interceptor.
> 3. **Portfolio Scope**: This project is published as an engineering portfolio and systems architecture demonstration, not as an active production SaaS deployment.

---

## Security Disclosure

During security testing, a password-recovery authorization flaw was identified in an earlier implementation. The recovery architecture was subsequently redesigned around server-side OTP verification, purpose-bound challenges, rate limiting, secure OTP storage, and session invalidation.

---

## License

**All Rights Reserved**

Redistribution and use in source and binary forms, with or without modification, are strictly prohibited without prior written permission from the copyright holder.

Public GitHub visibility does not grant permission to reproduce, modify, redistribute, or commercially use the original project code. Third-party frameworks, libraries, and dependencies remain the property of their respective copyright holders.

---

## Copyright

Copyright © 2026 Jai Sai Vardhan Reddy. All rights reserved.

