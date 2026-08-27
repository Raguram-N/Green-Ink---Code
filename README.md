# Green Ink REST API — Developer Review Baseline

Spring Boot backend skeleton for the current Green Ink TNPSC prototype.

This repository is intentionally designed so the **REST contract, security, business rules, and Controller → Service → Repository separation can be reviewed before the Supabase/Postgres schema is finalized**.

## Review status

- Java 21
- Spring Boot 3.5.13 baseline
- Spring Web + Validation + Security
- JWT access tokens
- opaque refresh token in HttpOnly cookie
- OTP provider abstraction
- guest/free access
- dynamic Premium entitlement guard
- six-unit / 200-chapter catalog copied from the current prototype structure
- exact current free-chapter rules copied into `catalog.json`
- Notes content repository interface with two small **DEV fixtures only**
- PYQ repository interface with small **DEV fixtures only**
- guest PYQ attempt token
- logged-user progress
- search + server-side search history for logged users
- profile + text-size/notification preferences
- notification-list endpoint (empty placeholder until a notification source/schema exists)
- plans matching current prototype: ₹199 / month, ₹799 / 6 months, ₹999 / year
- payment gateway abstraction + demo gateway
- payment verification activates Premium server-side
- Razorpay webhook route with demo verifier
- admin role guard example
- in-memory repository adapters only; replace after schema finalization

> This is a code-review baseline, **not a production deployment**. Real OTP, real Razorpay verification, durable persistence, distributed rate limiting, production secrets, observability, backups, and the final privacy/data-retention rules must be completed before launch.

---

## 1. Architecture

```text
Green Ink PWA
     |
     | HTTPS / JSON
     v
Spring Security + JWT
     |
Controllers
     |
Services / business rules
     |
Repository interfaces ---------------- PaymentGateway / OtpProvider
     |                                      |
Placeholder in-memory adapters              Demo adapters (dev)
     |
Future Supabase/Postgres adapters
```

### Important rule

The frontend can *show* a Premium lock, but it never decides Premium access.

```text
GET /api/v1/chapters/u1-c2/notes
          |
          v
ContentService
          |
          v
EntitlementService
          |
     FREE chapter? ---------- yes -> allow
          |
          no
          |
logged-in user + active subscription? -> allow
          |
          no
          v
403 PREMIUM_REQUIRED
```

---

## 2. Run locally

Requirements:

- JDK 21
- Maven 3.9+

Run with the dev profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Server:

```text
http://localhost:8787
```

Health:

```bash
curl http://localhost:8787/actuator/health
```

### Dev-only credentials

OTP:

```text
1234
```

The dev OTP is also returned as `debugOtp` from the OTP request endpoint when the `dev` profile is active.

Demo payment verification signature:

```text
dev-valid-signature
```

Demo webhook signature:

```text
dev-webhook-signature
```

Admin identifier for guard review:

```text
admin@greenink.local
```

Never copy these values into production configuration.

---

## 3. Authentication model

### Guest

No token is required for:

- catalog browsing
- search
- free chapter Notes
- free chapter PYQ

Guest PYQ attempts receive a random `guestAttemptToken`. The token must be sent as:

```http
X-Attempt-Token: <token>
```

for subsequent answer/complete calls.

### Logged user

OTP verification returns a short-lived JWT access token.

Send it as:

```http
Authorization: Bearer <access-token>
```

The refresh token is an opaque random secret stored as a hash in `SessionRepository` and sent to the browser as an HttpOnly cookie.

### Roles

```text
ROLE_USER   verified account
ROLE_ADMIN  future administration / content operations
```

Premium is deliberately **not** a Spring role. It is an expiring entitlement checked through `EntitlementService`.

---

## 4. Endpoint matrix

| Method | Endpoint | Guard | Purpose |
|---|---|---|---|
| POST | `/api/v1/auth/otp/request` | Public | Issue OTP challenge |
| POST | `/api/v1/auth/otp/resend` | Public + provider cooldown (429 when too soon) | Issue replacement OTP challenge |
| POST | `/api/v1/auth/otp/verify` | Public | Verify OTP, create user if needed, issue session |
| POST | `/api/v1/auth/refresh` | Refresh cookie | Rotate refresh token and issue access token |
| POST | `/api/v1/auth/logout` | Public | Invalidate current refresh token |
| POST | `/api/v1/auth/logout-all` | USER | Invalidate all refresh sessions |
| GET | `/api/v1/units` | Public | All units + chapters |
| GET | `/api/v1/units/{unitId}` | Public | One unit |
| GET | `/api/v1/units/{unitId}/chapters` | Public | Unit chapters |
| GET | `/api/v1/chapters/{chapterId}` | Public | Chapter metadata/access state |
| GET | `/api/v1/chapters/{chapterId}/notes` | Free chapter OR active Premium | Full chapter Notes document |
| POST | `/api/v1/chapters/{chapterId}/pyq/attempts` | Free chapter OR active Premium | Start PYQ attempt |
| POST | `/api/v1/pyq/attempts/{attemptId}/answers` | Attempt owner / guest attempt token | Submit one answer |
| POST | `/api/v1/pyq/attempts/{attemptId}/complete` | Attempt owner / guest attempt token | Complete attempt and save logged-user progress |
| GET | `/api/v1/search?q=...` | Public | Search chapter catalog |
| GET | `/api/v1/me/search-history` | USER | Recent synced searches |
| DELETE | `/api/v1/me/search-history/{historyId}` | USER | Delete one search |
| DELETE | `/api/v1/me/search-history` | USER | Clear search history |
| GET | `/api/v1/me` | USER | Profile + Premium + progress + notification count |
| GET | `/api/v1/me/notifications` | USER | Notification list |
| GET | `/api/v1/me/preferences` | USER | Text-size/notification preferences |
| PUT | `/api/v1/me/preferences` | USER | Update preferences |
| DELETE | `/api/v1/me` | USER | Delete current review-stage account state |
| GET | `/api/v1/me/progress` | USER | Notes/PYQ progress |
| PUT | `/api/v1/me/progress/chapters/{chapterId}` | USER | Mark Notes complete/incomplete |
| DELETE | `/api/v1/me/progress/notes` | USER | Reset Notes progress |
| DELETE | `/api/v1/me/progress/pyq` | USER | Reset PYQ progress |
| GET | `/api/v1/plans` | Public | Current Premium plans |
| POST | `/api/v1/billing/orders` | USER | Create provider order |
| POST | `/api/v1/billing/payments/verify` | USER + payment owner | Verify browser checkout result |
| GET | `/api/v1/billing/subscription` | USER | Current entitlement |
| GET | `/api/v1/billing/payments` | USER | Payment history |
| POST | `/api/v1/webhooks/razorpay` | Provider signature | Payment webhook |
| GET | `/api/v1/admin/review` | ADMIN | Demonstrates role guard |

### Why there is no `/notes/topics/{topicId}` yet

The current prototype already renders chapter Notes as HTML and derives topic navigation from that document. Splitting Notes into a topic-table/API now would prematurely lock the database/content schema.

For the review phase the API returns:

```json
{
  "chapterId": "u1-c1",
  "version": "...",
  "format": "HTML_FRAGMENT",
  "body": "<section>...</section>"
}
```

Once the content schema is finalized, `ContentRepository` can change internally without changing the controller/service contract. If the production frontend truly needs server-addressable topics, add them at that point.

---

## 5. Prototype catalog access rules

The generated `catalog.json` contains the current six units and 200 chapters.

Current free chapter numbers are:

```text
Unit I   : 1, 13, 30, 35
Unit II  : 1, 8, 18, 25
Unit III : 1, 10, 22, 28
Unit IV  : 1, 3, 8, 20
Unit V   : 1, 7, 26, 44
Unit VI  : 1, 2, 9, 28
```

Everything else is `PREMIUM`.

The prototype currently contains **6,099 mapped PYQ questions**. `pyq-metadata.json` preserves the existing per-unit totals (830, 638, 1,341, 1,084, 1,280, 926) so server-side progress can remain question-based even though this review repository contains only a few PYQ fixtures.

The client may use the returned `access` and `accessible` fields to draw locks, but the server checks entitlement again before returning Notes or starting PYQ.

---

## 6. End-to-end curl flow

### A. Guest catalog

```bash
curl http://localhost:8787/api/v1/units
```

### B. Guest opens a free Notes fixture

```bash
curl http://localhost:8787/api/v1/chapters/u1-c1/notes
```

### C. Guest tries Premium Notes

```bash
curl -i http://localhost:8787/api/v1/chapters/u1-c2/notes
```

Expected:

```json
{
  "status": 403,
  "code": "PREMIUM_REQUIRED",
  "message": "This chapter requires Green Ink Premium."
}
```

### D. Request OTP

```bash
curl -X POST http://localhost:8787/api/v1/auth/otp/request \
  -H "Content-Type: application/json" \
  -d '{"identifier":"9876543210"}'
```

Copy `challengeId` from the response.

### E. Verify OTP

```bash
curl -i -X POST http://localhost:8787/api/v1/auth/otp/verify \
  -H "Content-Type: application/json" \
  -d '{"challengeId":"<challenge-id>","otp":"1234"}'
```

Copy `accessToken`.

### F. Profile

```bash
curl http://localhost:8787/api/v1/me \
  -H "Authorization: Bearer <access-token>"
```

### G. Create yearly payment order

```bash
curl -X POST http://localhost:8787/api/v1/billing/orders \
  -H "Authorization: Bearer <access-token>" \
  -H "Content-Type: application/json" \
  -d '{"planCode":"YEARLY"}'
```

Copy `orderId`.

### H. Demo payment verification

```bash
curl -X POST http://localhost:8787/api/v1/billing/payments/verify \
  -H "Authorization: Bearer <access-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId":"<order-id>",
    "paymentId":"demo_pay_1",
    "signature":"dev-valid-signature"
  }'
```

Premium is now stored server-side in the placeholder subscription repository.

### I. Open Premium Notes again

```bash
curl http://localhost:8787/api/v1/chapters/u1-c2/notes \
  -H "Authorization: Bearer <access-token>"
```

### J. Mark Notes complete

```bash
curl -X PUT http://localhost:8787/api/v1/me/progress/chapters/u1-c1 \
  -H "Authorization: Bearer <access-token>" \
  -H "Content-Type: application/json" \
  -d '{"notesCompleted":true}'
```

---

## 7. PYQ security flow

The correct answer is deliberately omitted when an attempt starts.

Start free guest attempt:

```bash
curl -X POST http://localhost:8787/api/v1/chapters/u1-c1/pyq/attempts
```

Response contains:

```json
{
  "attemptId": "att_...",
  "guestAttemptToken": "...",
  "questions": [
    {
      "id": "q_u1c1_1",
      "question": "...",
      "options": [ ... ]
    }
  ]
}
```

There is no `correctOption` in the question payload.

Submit:

```bash
curl -X POST http://localhost:8787/api/v1/pyq/attempts/<attempt-id>/answers \
  -H "X-Attempt-Token: <guest-attempt-token>" \
  -H "Content-Type: application/json" \
  -d '{"questionId":"q_u1c1_1","selectedOption":"B"}'
```

Only after answer submission does the response reveal correctness, correct option, and explanation.

Logged-user attempts are bound to the account instead of an `X-Attempt-Token`.

---

## 8. Repository layer — intentionally placeholder

Interfaces:

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

Review-stage adapters are under:

```text
com.greenink.api.infrastructure.memory
```

When the schema is ready, replace these adapters. Do **not** rewrite controllers to call Supabase directly.

See `docs/REPOSITORY_HANDOFF.md`.

---

## 9. Payment production work

The demo flow proves the service boundaries and entitlement activation logic. Before production, add a real `RazorpayPaymentGateway` implementation that:

1. creates orders using Razorpay server credentials;
2. never accepts amount from the frontend — amount comes from `PlanService`;
3. verifies checkout signatures server-side;
4. verifies webhook HMAC using the raw request body;
5. handles webhook retries idempotently;
6. records provider event/payment IDs uniquely;
7. handles failed/refunded payments and subscription state changes;
8. keeps secrets in environment/secret management only.

The client must never write `premium=true` itself.

---

## 10. OTP production work

Replace `DevOtpProvider` with the selected production provider.

Production requirements:

- provider-side OTP generation/verification where possible;
- hashed/opaque challenge data where stored locally;
- Redis/database-backed cooldown and abuse limits;
- IP/device/account rate limits;
- OTP expiry;
- attempt limits;
- audit/security logging without logging OTP values;
- mobile/email normalization;
- no `debugOtp` in production.

---

## 11. Configuration

Production environment variables at minimum:

```text
GREEN_INK_JWT_SECRET=<strong random 32+ byte secret>
GREEN_INK_SECURE_COOKIES=true
GREEN_INK_AUTH_MODE=<real-provider-mode>
GREEN_INK_BILLING_MODE=<razorpay-mode>
GREEN_INK_ALLOWED_ORIGINS=https://greenink.in
```

The application deliberately fails at startup if the JWT secret is shorter than 32 bytes.

---

## 12. Tests

```bash
mvn test
```

`ApiFlowIntegrationTest` exercises:

- six-unit catalog
- free Notes as guest
- Premium rejection as guest/free user
- OTP login
- profile authentication
- demo payment order + verification
- server-side Premium activation
- Premium content after activation
- Notes progress
- guest PYQ attempt ownership token
- no answer key in initial PYQ question payload

---

## 13. Developer review checklist

Please review these decisions before schema work begins:

- Is returning complete chapter Notes as `HTML_FRAGMENT` acceptable for the first production API?
- Should refresh sessions support multiple devices and named-device revocation?
- Which OTP provider will be used for mobile/email?
- Should guest PYQ stay server-backed with an attempt token, or remain entirely client-local until login?
- What is the authoritative definition of PYQ completion percentage?
- Should search cover chapter titles only initially, or index full Notes content?
- How should existing anonymous/local progress be migrated after a user logs in?
- What is the account deletion/data-retention policy for payment records?
- What exact Razorpay model is intended: one-time prepaid periods or recurring subscriptions?
- Should plans remain code/config based or become admin-managed data?
- What content version/cache invalidation contract should the PWA use?

Those questions affect schema design; they should be answered before creating final tables.
