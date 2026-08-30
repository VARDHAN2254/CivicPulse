# Changelog

All notable changes to the CivicPulse platform are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2026-08-30

### Portfolio Release — v1.0.0

### Added
- **Core Platform Architecture**: Spring Boot 3.3.2 backend (Java 21) and Next.js 14 App Router frontend (TypeScript, Tailwind CSS).
- **Authentication & RBAC**: JWT authentication with HMAC-SHA256 signing, BCrypt password hashing, and role hierarchy (`SUPER_ADMIN`, `ADMIN`, `MODERATOR`, `ORGANIZER`, `MEMBER`).
- **Organization Multi-Tenancy**: Organization creation, slug routing, membership roles (`OWNER`, `ORGANIZER`, `MEMBER`), and permission boundaries.
- **Event Lifecycle State Machine**: Full event management with state transitions (`DRAFT` -> `PUBLISHED` -> `COMPLETED` / `CANCELLED`), venue details, capacity limits, and categorization.
- **Zero-Overselling Concurrency Engine**: Database-level pessimistic locking (`SELECT ... FOR UPDATE`) preventing race conditions during ticket checkout.
- **FIFO Waitlist Engine**: Automated ticket promotion upon registration cancellation with real-time alert dispatch.
- **Cryptographic Rolling QR Attendance**: Dynamic HMAC-SHA256 attendance tokens (120s TTL) with duplicate check-in prevention and mobile organizer scanner terminal.
- **Distributed Event Streaming**: Apache Kafka (KRaft mode) event streams across 7 partitioned domain topics.
- **Real-Time STOMP WebSockets**: Broadcast channels for live organizer updates and user-targeted notifications.
- **Faceted Trigram Search**: PostgreSQL `pg_trgm` full-text search with faceted filtering by category, event format, and location.
- **Content Moderation & Safety**: Profanity/toxicity filtering and moderator triage queue for community discussion boards.
- **OTP-Centered Zero-Trust Authentication & Recovery**: Cryptographic 8-digit numeric OTP lifecycle (5-minute TTL, single-use, 5-attempt limit, 60s cooldown) for registration activation, password recovery, email changes, and step-up verification.
- **Account Enumeration Defense**: Constant-time `/forgot-password` response with timing normalization and zero secret leakage.
- **Multi-Factor Authentication (MFA)**: Enforced secondary factor for platform Administrators and TOTP authenticator enrollment with encrypted backup recovery codes.
- **Observability & Metrics**: Prometheus metric endpoints (`/actuator/prometheus`), custom business counters/timers, Spring Boot Actuator health probes, and provisioned Grafana dashboards.
- **Automated Test Coverage**: Full suite of unit, integration, and security tests, full Next.js production build with 26 routes, and Playwright end-to-end verification suite.

### Known Limitations
- **External Email Delivery**: External OTP/email delivery has not been verified against a real inbox in this release. Local development email testing may be configured where supported.
- **Portfolio Project**: Published as a portfolio and systems engineering showcase, not as an active production SaaS deployment.

