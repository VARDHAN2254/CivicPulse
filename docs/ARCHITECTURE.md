# CivicPulse Architecture & Domain Specifications

This document outlines the distributed architecture, domain state machine, streaming topology, and cryptographic attendance protocols of CivicPulse.

---

## 1. System Topology

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

## 2. Event Lifecycle State Machine

Events adhere to a strict deterministic state progression:

* `DRAFT`: Newly initialized event. Modifications allowed. Not publicly visible or indexable.
* `PUBLISHED`: Open for public discovery and registration bookings.
* `COMPLETED`: Concluded event. Registrations and check-ins locked; analytics finalized.
* `CANCELLED`: Explicitly terminated event. Registrations refunded/invalidated and automated alerts dispatched.

---

## 3. High-Concurrency Zero-Overselling Engine

To ensure capacity limits are strictly upheld under flash crowd conditions:
1. **Pessimistic DB Locks**: Ticket reservation executes within an isolated database transaction with row-level locks (`SELECT ... FOR UPDATE`).
2. **Atomic Counter Decrement**: If available capacity $> 0$, registration status is committed as `CONFIRMED`.
3. **FIFO Waitlist Spillover**: When capacity is saturated, registration is enrolled in the waitlist at ordinal position $N+1$.
4. **Automated Promotion**: Upon a cancellation, the waitlist promotion engine immediately claims the freed inventory for the head of the waitlist queue.

---

## 4. Cryptographic Dynamic QR Attendance Protocol

* **Payload Structure**: `{registrationId}.{timestampEpochSeconds}.{hmacSha256Signature}`
* **TTL Window**: 120 seconds. The frontend refreshes the rotating pass every 60 seconds.
* **Verification Algorithm**:
  1. Parse registration ID and timestamp.
  2. Compute expected HMAC-SHA256 signature using the server secret key.
  3. Validate cryptographic match (constant-time comparison).
  4. Verify that current server timestamp falls within $|T_{current} - T_{token}| \le 120\text{ seconds}$.
  5. Check database attendance records for prior check-in; reject duplicates.
  6. Atomically persist attendance check-in timestamp.

---

## 5. Apache Kafka Streaming Topology

| Topic | Partitions | Purpose |
| :--- | :--- | :--- |
| `civicpulse.events.lifecycle` | 6 | Event state transitions (`CREATED`, `PUBLISHED`, `CANCELLED`) |
| `civicpulse.registrations` | 12 | Ticket reservations, cancellations, and waitlist joins |
| `civicpulse.attendance` | 6 | Check-in verification audit trail |
| `civicpulse.notifications` | 6 | Asynchronous push notifications and email triggers |
| `civicpulse.announcements` | 6 | Organizer broadcast alerts |
| `civicpulse.moderation` | 3 | Flagged post queues and toxicity filter intercepts |
| `civicpulse.audit` | 6 | Immutable security audit log |
