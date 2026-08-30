# Green Ink REST API

Spring Boot backend for the **Green Ink TNPSC preparation app**.

Green Ink is a mobile-first study platform built around:

- structured TNPSC Notes
- PYQ+ practice
- free and Premium chapter access
- OTP login + guest access
- progress tracking
- search/history
- profile preferences
- Premium subscriptions and payments

The backend owns authentication, entitlement checks, payment verification, progress, and content access. The frontend may display locks and UI state, but it is **never the authority for Premium access**.

---

## Project status

Current backend stage:

- Java 21
- Spring Boot 3.5.13
- Spring Web + Validation + Security
- Controller → Service → Repository architecture
- configurable API base path
- JWT access-token authentication
- refresh-session abstraction
- OTP provider abstraction
- guest/free access
- server-side Premium entitlement checks
- six TNPSC units / 200 chapters
- 6,099 mapped PYQ questions in the current catalog metadata
- free-chapter rules implemented
- Notes repository contract implemented with DEV fixtures
- PYQ repository contract implemented with DEV fixtures
- guest PYQ attempt token
- logged-user Notes/PYQ progress
- search + synced search history
- profile preferences
- notification endpoint placeholder
- Premium plans
- demo payment gateway
- server-side Premium activation after payment verification
- Razorpay webhook route with demo verifier
- admin role-guard example
- in-memory repository adapters currently used
- database schema designed and reviewed
- production database implementation still pending

The current backend flow has also been manually verified end-to-end for:

- guest catalog access
- free Notes access
- Premium Notes rejection for non-Premium users
- OTP request + verification
- authenticated profile
- demo yearly order creation
- demo payment verification
- server-side Premium activation
- Premium Notes access after activation
- Notes progress

> **Important:** This repository is still a development / integration baseline, not a production deployment. Real database persistence, real OTP delivery, real Razorpay verification/webhooks, production secrets, rate limiting, monitoring, backup/recovery, and final security hardening are still required before launch.

---

## 1. Architecture

```text
Green Ink Frontend / PWA
        |
        | HTTPS / JSON
        v
Spring Security
        |
Controllers
        |
Services / Business Rules
        |
Repository Interfaces
        |
Current: In-memory adapters
Future : Real database adapters
        |
        +--------------------+
        |                    |
        v                    v
   Database              External Services
                         - OTP Provider
                         - Razorpay
```

### Core backend rule

The frontend may show a lock, but it never decides whether content is accessible.

```text
GET {API_BASE}/chapters/u1-c2/notes
              |
              v
        ContentService
              |
              v
      EntitlementService
              |
       Free chapter?
        /          \
      yes           no
      |             |
    allow      Logged in + active Premium?
                    /          \
                  yes           no
                  |             |
                allow      403 PREMIUM_REQUIRED
```

---

## 2. API base path is configurable

The API version/base path is no longer hardcoded inside controllers.

Default:

```text
/api/v1
```

Configuration:

```yaml
greenink:
  api:
    base-path: ${GREEN_INK_API_BASE_PATH:/api/v1}
```

Override example:

```text
GREEN_INK_API_BASE_PATH=/api/v2
```

Controllers, security matchers, and authentication cookie paths use the configured base path.

This allows the project to move from `/api/v1` to a future API version without editing every controller.

Throughout this README, `/api/v1` means the **current default API base path**.

---

## 3. Run locally

### Requirements

- JDK 21
- Maven 3.9+

### Start with the dev profile

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Or in PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
mvn spring-boot:run
```

Server:

```text
http://localhost:8787
```

Health:

```bash
curl http://localhost:8787/actuator/health
```

---

## 4. Dev-only credentials

### OTP

```text
1234
```

The dev OTP may be returned as `debugOtp` while the `dev` profile is active.

### Demo payment verification signature

```text
dev-valid-signature
```

### Demo webhook signature

```text
dev-webhook-signature
```

### Admin identifier

```text
admin@greenink.local
```

**Never use any of these development values in production.**

---

## 5. Authentication model

### Guest users

No access token is required for:

- catalog browsing
- search
- free chapter Notes
- free chapter PYQ+

Guest PYQ attempts receive a random attempt token.

Send it in subsequent answer/complete requests:

```http
X-Attempt-Token: <guest-attempt-token>
```

### Logged users

OTP verification creates/authenticates the user and returns a short-lived JWT access token.

```http
Authorization: Bearer <access-token>
```

The current application also has a refresh-session abstraction.

The target database design uses a **single-device session model** with:

```text
active_session_id
active_device_hash
```

Purpose:

- allow the latest valid session to displace an older session
- reduce account sharing
- make a stolen cookie less useful from another device
- avoid unreliable IP locking on mobile networks

The final repository/session implementation will be completed when the real database layer is wired.

### Roles

```text
ROLE_USER
ROLE_ADMIN
```

Premium is **not** a Spring role.

Premium is an expiring entitlement checked by backend business logic.

---

## 6. Premium plans

Current Green Ink pricing:

```text
₹199 / Month
₹799 / 6 Months
₹999 / Year
```

The yearly plan is currently positioned as the best-value option in the frontend.

The backend remains the authority for:

- plan code
- amount
- entitlement duration
- subscription activation
- subscription expiry

The frontend must never send or set `premium=true` as trusted state.

---

## 7. Catalog and chapter access

The current catalog contains:

```text
6 Units
200 Chapters
```

Current free chapter numbers:

```text
Unit I   : 1, 13, 30, 35
Unit II  : 1, 8, 18, 25
Unit III : 1, 10, 22, 28
Unit IV  : 1, 3, 8, 20
Unit V   : 1, 7, 26, 44
Unit VI  : 1, 2, 9, 28
```

Everything else is Premium.

The current catalog metadata also preserves **6,099 mapped PYQ questions**:

```text
Unit I   :   830
Unit II  :   638
Unit III : 1,341
Unit IV  : 1,084
Unit V   : 1,280
Unit VI  :   926
Total    : 6,099
```

The frontend can use `access` / `accessible` to render locks, but Spring checks entitlement again before returning protected Notes or starting protected PYQ attempts.

---

## 8. Endpoint matrix

All paths below use the current default API base path `/api/v1`.

| Method | Endpoint | Guard | Purpose |
|---|---|---|---|
| POST | `/api/v1/auth/otp/request` | Public | Issue OTP challenge |
| POST | `/api/v1/auth/otp/resend` | Public + cooldown | Issue replacement OTP challenge |
| POST | `/api/v1/auth/otp/verify` | Public | Verify OTP and issue session |
| POST | `/api/v1/auth/refresh` | Refresh session | Rotate/refresh authentication |
| POST | `/api/v1/auth/logout` | Public/current session | Logout current session |
| POST | `/api/v1/auth/logout-all` | USER | Invalidate user sessions |
| GET | `/api/v1/units` | Public | All units + chapters |
| GET | `/api/v1/units/{unitId}` | Public | One unit |
| GET | `/api/v1/units/{unitId}/chapters` | Public | Chapters in a unit |
| GET | `/api/v1/chapters/{chapterId}` | Public | Chapter metadata/access state |
| GET | `/api/v1/chapters/{chapterId}/notes` | Free OR Premium | Chapter Notes |
| POST | `/api/v1/chapters/{chapterId}/pyq/attempts` | Free OR Premium | Start PYQ attempt |
| POST | `/api/v1/pyq/attempts/{attemptId}/answers` | Attempt owner | Submit one answer |
| POST | `/api/v1/pyq/attempts/{attemptId}/complete` | Attempt owner | Complete attempt |
| GET | `/api/v1/search?q=...` | Public | Search |
| GET | `/api/v1/me/search-history` | USER | Synced search history |
| DELETE | `/api/v1/me/search-history/{historyId}` | USER | Delete one search |
| DELETE | `/api/v1/me/search-history` | USER | Clear search history |
| GET | `/api/v1/me` | USER | Profile + Premium + progress |
| GET | `/api/v1/me/notifications` | USER | Notification list |
| GET | `/api/v1/me/preferences` | USER | User preferences |
| PUT | `/api/v1/me/preferences` | USER | Update preferences |
| DELETE | `/api/v1/me` | USER | Delete current account state |
| GET | `/api/v1/me/progress` | USER | Notes/PYQ progress |
| PUT | `/api/v1/me/progress/chapters/{chapterId}` | USER | Mark Notes complete/incomplete |
| DELETE | `/api/v1/me/progress/notes` | USER | Reset Notes progress |
| DELETE | `/api/v1/me/progress/pyq` | USER | Reset PYQ progress |
| GET | `/api/v1/plans` | Public | Premium plans |
| POST | `/api/v1/billing/orders` | USER | Create payment order |
| POST | `/api/v1/billing/payments/verify` | USER + owner | Verify checkout result |
| GET | `/api/v1/billing/subscription` | USER | Current subscription/entitlement |
| GET | `/api/v1/billing/payments` | USER | Payment history |
| POST | `/api/v1/webhooks/razorpay` | Provider signature | Razorpay webhook |
| GET | `/api/v1/admin/review` | ADMIN | Admin role-guard example |

---

## 9. Notes model

The Green Ink frontend presents Notes as structured exam-oriented cards with:

- numbered topics
- concise factual bullets
- Must-Memorise blocks
- TNPSC Favourite blocks
- TNPSC Trap blocks
- Rapid Revision
- direct navigation to PYQ+

The existing development backend currently returns Notes through the content repository contract.

Current review response shape:

```json
{
  "chapterId": "u1-c1",
  "version": "...",
  "format": "HTML_FRAGMENT",
  "body": "<section>...</section>"
}
```

### Target schema direction

The reviewed database schema introduces structured chapter content through the syllabus/content model.

The final content-storage implementation must preserve the current Green Ink Notes UI without forcing frontend redesign.

Production Notes are **not yet migrated into the real database**.

The current repository still uses development fixtures.

---

## 10. PYQ+ model

Green Ink combines Notes and PYQ+ inside the same chapter experience.

The target database design uses:

- one master `questions` record per unique question
- flat option columns (`opt_1` to `opt_4`)
- `correct_option_key`
- question type
- explanation
- `question_banks`
- `bank_questions_map`
- `question_appearances`

This allows one question to:

- appear in multiple practice/mock/PYQ collections
- record multiple TNPSC exam/year appearances
- be corrected once without duplicating content

Supported schema-level question categories include:

```text
MCQ
ASSERTION_REASON
MATCH
MULTI_STATEMENT
```

The exact content convention for special formats must remain consistent with the Green Ink PYQ UI.

### PYQ answer security

The correct answer is not included in the initial attempt payload.

Example start response:

```json
{
  "attemptId": "att_...",
  "guestAttemptToken": "...",
  "questions": [
    {
      "id": "q_u1c1_1",
      "question": "...",
      "options": ["...", "...", "...", "..."]
    }
  ]
}
```

Correctness, correct option, and explanation are revealed only after the user submits an answer.

---

## 11. Database schema status

The Green Ink database schema has now been designed and reviewed as the main implementation baseline.

The database provider is intentionally **not hardcoded in the architecture**.

Important design decisions include:

### Flat question columns

```text
opt_1
opt_2
opt_3
opt_4
correct_option_key
```

Chosen for:

- predictable SQL
- type-safe repository models
- simple option shuffling
- avoiding unnecessary JSON parsing for large mocks
- database-level validation

### Cached entitlement on user

The user record keeps fast-access entitlement fields such as:

```text
cached_tier_level
tier_expires_at
```

The subscription ledger remains responsible for subscription history.

This gives fast everyday access checks while preserving billing history.

### Single-device session security

Target fields:

```text
active_session_id
active_device_hash
```

### Payment idempotency

Target payment design includes:

```text
idempotency_key
provider_event_id
```

Purpose:

- prevent accidental duplicate checkout creation
- prevent duplicate webhook processing
- prevent duplicate Premium grants

### Zero question duplication

```text
questions
       |
       +--> bank_questions_map --> question_banks
       |
       +--> question_appearances --> exam/year history
```

### Current implementation status

Schema design:

```text
Designed / reviewed
```

Real database tables:

```text
Pending
```

Repository integration:

```text
Pending
```

Current development persistence:

```text
In-memory adapters
```

---

## 12. Repository layer

Repository interfaces currently include:

```text
UserRepository
SessionRepository
CatalogRepository
ContentRepository
ProgressRepository
PyqRepository
PyqAttemptRepository
SearchHistoryRepository
SubscriptionRepository
PaymentRepository
NotificationRepository
```

Current adapters:

```text
com.greenink.api.infrastructure.memory
```

The next major backend milestone is replacing the in-memory implementations with real database-backed repositories.

Expected flow:

```text
Controller
    |
Service
    |
Repository Interface
    |
Database Adapter
    |
Database
```

Controllers and services should not be rewritten to directly depend on a specific database vendor.

---

## 13. Manual end-to-end API verification

The following development flow has already been tested successfully against the running Spring Boot application.

### 1. Catalog

```text
GET /api/v1/units
```

Result:

- all six units returned
- chapter catalog returned
- free/Premium access state returned

### 2. Free Notes

```text
GET /api/v1/chapters/u1-c1/notes
```

Result:

```text
200 OK
```

### 3. Premium Notes as free/guest user

```text
GET /api/v1/chapters/u1-c2/notes
```

Result:

```text
403 PREMIUM_REQUIRED
```

### 4. OTP login

```text
POST /api/v1/auth/otp/request
POST /api/v1/auth/otp/verify
```

Result:

- challenge issued
- dev OTP verified
- access token returned
- user profile accessible

### 5. Profile

```text
GET /api/v1/me
```

Result included:

- user identity
- roles
- Premium state
- preferences
- notifications count
- Notes/PYQ progress

### 6. Yearly demo order

```text
POST /api/v1/billing/orders
```

with:

```json
{
  "planCode": "YEARLY"
}
```

Verified development amount:

```text
₹999
```

### 7. Demo payment verification

```text
POST /api/v1/billing/payments/verify
```

Result:

- payment verified
- YEARLY entitlement activated
- Premium became active server-side

### 8. Premium Notes after activation

```text
GET /api/v1/chapters/u1-c2/notes
```

with the authenticated access token.

Result:

```text
200 OK
```

This verifies the core business flow:

```text
Free user
   ↓
Premium blocked
   ↓
Login
   ↓
Payment
   ↓
Backend activates entitlement
   ↓
Premium content unlocked
```

---

## 14. Payment architecture

Current implementation:

```text
PaymentGateway abstraction
        |
        v
Demo payment gateway
```

Production target:

```text
Frontend
   |
   v
Spring Boot
   |
   +--> PlanService decides amount
   |
   +--> Razorpay creates order
   |
   v
User checkout
   |
   v
Server-side verification
   |
   v
Webhook confirmation
   |
   v
Payment transaction
   |
   v
Subscription / entitlement
```

Production requirements:

1. create orders with Razorpay server credentials;
2. never trust payment amount from frontend;
3. verify checkout signatures server-side;
4. verify webhook HMAC using the raw request body;
5. process webhook retries idempotently;
6. store provider payment/event IDs uniquely;
7. handle failed/refunded payments;
8. update subscription and cached user entitlement consistently;
9. keep all provider secrets on the backend.

---

## 15. OTP production work

Current:

```text
Dev OTP provider
OTP = 1234
```

Production:

- select mobile/email OTP provider
- remove `debugOtp`
- enforce OTP expiry
- resend cooldown
- attempt limits
- abuse/rate limiting
- normalize mobile/email identifiers
- security logging without logging actual OTP values
- persistent challenge/session state where needed

---

## 16. Configuration

Minimum production configuration will include values such as:

```text
GREEN_INK_API_BASE_PATH=/api/v1
GREEN_INK_JWT_SECRET=<strong-random-secret>
GREEN_INK_SECURE_COOKIES=true
GREEN_INK_AUTH_MODE=<production-provider-mode>
GREEN_INK_BILLING_MODE=<razorpay-mode>
GREEN_INK_ALLOWED_ORIGINS=https://greenink.in
```

Future database connection settings will also be supplied through environment/secret configuration once the database provider is selected.

Secrets must not be committed to Git.

---

## 17. Tests

Run:

```bash
mvn test
```

The latest verified local run completed successfully:

```text
BUILD SUCCESS
```

The current integration tests cover the core development API flow, including:

- six-unit catalog
- free Notes access
- Premium rejection
- OTP login
- profile authentication
- demo payment order
- payment verification
- server-side Premium activation
- Premium content access after activation
- Notes progress
- guest PYQ ownership
- no answer key in initial PYQ payload

---

## 18. Target production deployment

Recommended final separation:

```text
https://greenink.in
        |
        v
Green Ink Frontend / PWA
        |
        | HTTPS REST API
        v
https://api.greenink.in
        |
        v
Spring Boot Docker Container
        |
        +--> Database
        |
        +--> OTP Provider
        |
        +--> Razorpay
```

GitHub is used for source control and deployment workflows.

It is **not** part of the normal end-user request path.

---

## 19. Remaining work before production

Major remaining tasks:

1. finalize any remaining schema-level clarifications;
2. choose the production database platform;
3. create database migrations/tables;
4. implement database-backed repository adapters;
5. import the real six-unit/chapter catalog;
6. migrate production Notes;
7. migrate the complete PYQ dataset;
8. connect the existing Green Ink frontend to the Spring APIs;
9. implement real OTP delivery/verification;
10. implement real Razorpay order/signature/webhook processing;
11. persist all required progress/preferences/search state;
12. finalize session lifecycle/security;
13. implement production rate limiting;
14. secure secrets and environment configuration;
15. add observability/logging;
16. define backup/recovery strategy;
17. define account deletion and data-retention rules;
18. deploy frontend + backend;
19. test on real devices/networks;
20. complete final end-to-end security and payment QA.

---

## 20. Next engineering milestone

The immediate next major backend milestone is:

```text
Final schema
     ↓
Create real database
     ↓
Implement repository adapters
     ↓
Import catalog / Notes / PYQ
     ↓
Connect frontend
```

Once the repository layer is backed by a real database, Green Ink moves from a working backend prototype to a persistent application backend.

---

## 21. Development principles

Green Ink backend should continue following these rules:

- Controllers handle HTTP concerns only.
- Services own business rules.
- Repositories own persistence.
- Premium access is always enforced server-side.
- Payment amount is always decided server-side.
- Payment callbacks/webhooks must be idempotent.
- Sensitive secrets stay on the server.
- The frontend never becomes the source of truth for Premium.
- Questions should not be duplicated unnecessarily.
- User progress must survive logout/restart once real persistence is enabled.
- Database-provider details should remain behind repository interfaces.
- Existing Green Ink frontend behavior should not be changed unnecessarily during backend migration.

---

## Current summary

```text
Frontend prototype             Built / polished
Spring REST architecture       Built
API routing                    Built
Dynamic API base path          Built
Authentication flow            Built in development mode
Premium access guard           Built
Demo payment flow              Built and verified
Catalog                        Built
Notes/PYQ repository contract  Built
Database schema                Designed / reviewed
Real database repositories     Pending
Production Notes/PYQ storage   Pending
Frontend ↔ backend integration Pending
Real OTP                       Pending
Real Razorpay                  Pending
Production deployment          Pending
```

Green Ink is now past the pure prototype stage. The remaining work is mainly converting the reviewed architecture and schema into **durable production infrastructure**, connecting the existing frontend to it, and replacing development adapters with real production services.
