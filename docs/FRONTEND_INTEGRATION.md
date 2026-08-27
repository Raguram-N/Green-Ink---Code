# Frontend integration — replacing prototype-local authority

The current prototype keeps several production-important values in browser memory/localStorage. Once this API is connected, those values may still be used as UI caches, but they must no longer be authoritative.

| Current prototype state | Production authority | API |
|---|---|---|
| `state.authMode` / `state.accountId` | authenticated session | `/auth/otp/*`, JWT + refresh cookie |
| `greenink.access-tier.v1` | server subscription | `/billing/subscription` and `EntitlementService` |
| `greenink.premium-account.v1` | server user/subscription relation | `/me`, `/billing/subscription` |
| `greenink.premium-activated-at.v1` | server subscription timestamps | `/billing/subscription` |
| `greenink.profile-progress.v1.completed` | server Notes progress | `/me/progress` |
| `greenink.profile-progress.v1.textSize` | server preference for logged user | `/me/preferences` |
| in-memory `pyqSessions` | server attempt + progress | `/chapters/{id}/pyq/attempts`, `/pyq/attempts/*` |
| `greenink.search-history.v1` | server history for logged user | `/me/search-history` |
| `greenInkNotificationsV1` | future notification repository | `/me/notifications` |

## Recommended startup flow for a logged user

```text
PWA loads
  -> POST /auth/refresh (HttpOnly refresh cookie)
  -> receive access token
  -> GET /me
  -> GET /units (with bearer token so completed/access fields are personalized)
```

If refresh fails with 401, show the login/guest screen.

## Premium chapter click

```text
GET /chapters/{chapterId}
  accessible=false
       |
       v
show existing Premium sheet
       |
       v
user pays
       |
POST /billing/orders
Razorpay checkout
POST /billing/payments/verify
       |
       v
GET /billing/subscription
       |
       v
retry GET /chapters/{chapterId}/notes
```

Do not set Premium locally after a browser-side payment-success callback. Only unlock after the backend verifies and reports an active entitlement.

## Guest behavior

Guest browsing does not need an auth endpoint. Free chapter Notes can be fetched anonymously. A free guest PYQ attempt returns a `guestAttemptToken`; keep that token only for the life of the attempt and send it as `X-Attempt-Token`.

Guest search history and text-size may remain device-local if desired. When a user logs in, decide explicitly whether local progress/history should be merged into the account; that merge policy is intentionally not invented in this review baseline.

## Content rendering

The first API contract returns Notes as an `HTML_FRAGMENT`. This is intentional because the current frontend already renders finalized HTML Notes and topic navigation. It avoids forcing a topic/table database schema before the schema review is complete.
