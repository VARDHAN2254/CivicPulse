# Security Policy & Architecture — CivicPulse

CivicPulse is engineered with defense-in-depth security principles across authentication, authorization, multi-tenancy, rate limiting, cryptographic ticket verification, and zero-trust account recovery.

---

## 1. Security Architecture

### Authentication & Session Management
- **Stateless JWT Sessions**: Authentication uses HMAC-SHA256 signed JSON Web Tokens (`Authorization: Bearer <token>`) containing user identity, claims, and role grants.
- **BCrypt Password Hashing**: User credentials are encrypted using BCrypt with a work factor of 12 rounds.
- **Session & Token Invalidation**: Refresh tokens support revocation upon explicit logout. Password changes and security credential updates immediately invalidate active refresh tokens, forcing re-authentication across all active sessions.

### Zero-Trust OTP Verification
- **Cryptographic OTP Generation**: Sensitive identity operations (registration verification, password recovery, email address modifications, and privileged operations) utilize cryptographically random 8-digit numeric One-Time Passwords generated via `java.security.SecureRandom`.
- **Hashed OTP Storage**: OTPs are stored exclusively as SHA-256 hashes in the database with a 5-minute time-to-live (TTL). Raw OTP values are never logged, never persisted in plaintext, and never exposed in API responses.
- **Throttling & Single-Use Authorization**: Each OTP challenge permits a maximum of 5 failed attempts before invalidation. A 60-second resend cooldown is strictly enforced. Upon successful OTP verification, single-use, purpose-bound authorization tokens are issued for credential updates.
- **Account Enumeration Defense**: The `/api/v1/auth/forgot-password` endpoint returns an identical, constant-time generic response regardless of whether an account exists, eliminating timing side-channel attacks.

### Multi-Factor Authentication (MFA)
- **Privileged Account MFA**: Administrative accounts enforce secondary factor challenges.
- **TOTP Authenticator Support**: Users can enroll time-based one-time password (TOTP) authenticator applications with encrypted backup recovery codes.

### Role-Based Access Control (RBAC) & Multi-Tenancy
- **Hierarchical Roles**: `SUPER_ADMIN`, `ADMIN`, `MODERATOR`, `ORGANIZER`, `MEMBER`.
- **Multi-Tenant Isolation**: Event management, organization settings, and attendee check-in endpoints strictly verify that the authenticated user holds valid ownership or management permissions within the host organization.

### Distributed Rate Limiting
- **Redis Sliding-Window Protection**: Endpoints across authentication, ticket registration, and attendance check-in enforce rate limits backed by Redis to prevent brute-force attacks and resource exhaustion.

### Input Sanitization & Content Moderation
- **Cross-Site Scripting (XSS) Prevention**: User-submitted content (titles, descriptions, discussion posts) is sanitized to strip malicious HTML and JavaScript injection patterns.
- **Automated Safety Filters**: Keyword and toxicity scanners automatically flag inappropriate content for moderator triage.

### Cryptographic QR Dynamic Attendance
- **HMAC-SHA256 Rolling Passes**: Check-in QR passes use time-expiring HMAC-SHA256 signatures with a 120-second TTL to prevent replay attacks and screenshot sharing.
- **Duplicate Check-In Guard**: The scanner endpoint atomically checks and flags duplicate admissions.

### Audit Logging & Telemetry
- **Domain Event Streaming**: High-privilege actions, security changes, and event state transitions are streamed to dedicated Kafka audit topics (`civicpulse.audit`). Raw secrets and OTPs are strictly excluded.

---

## 2. Historical Security Remediation

During security testing, a password-recovery authorization flaw was identified in an earlier implementation. The recovery architecture was subsequently redesigned around server-side OTP verification, purpose-bound challenges, rate limiting, secure OTP storage, and session invalidation.

---

## 3. Known Limitation Disclosure

> [!WARNING]
> **Known Limitation**: The OTP generation and verification flow is implemented, but external SMTP/email delivery to a real inbox remains unverified in the current release. Local development email testing may be used where configured.

The platform is published as a portfolio engineering showcase and is not currently operated as a public production service.

---

## 4. Reporting a Security Vulnerability

If you discover a potential security vulnerability in this project, please report it responsibly by opening a confidential Security Advisory on GitHub or contacting the repository owner. Please allow reasonable time for investigation and remediation before public disclosure.

