# Repository handoff after schema finalization

The current `Placeholder*Repository` classes are deliberately in-memory. The Controller and Service layers should not change when Supabase/Postgres is introduced.

Implement adapters for these interfaces:

- `UserRepository`
- `SessionRepository`
- `ContentRepository`
- `ProgressRepository`
- `PyqRepository`
- `PyqAttemptRepository`
- `SearchHistoryRepository`
- `SubscriptionRepository`
- `PaymentRepository`
- `NotificationRepository`

`CatalogRepository` currently reads `catalog.json`, which was generated from the current prototype's six units and 200 chapters. It may remain classpath-backed or later become database-backed.

Do not place Supabase SQL, table names, or JPA entities in the Controller/Service packages. Database-specific mapping belongs in an infrastructure adapter package.

## Suggested future package

```text
infrastructure/
  supabase/
    SupabaseUserRepository.java
    SupabaseContentRepository.java
    SupabaseProgressRepository.java
    ...
```

The production implementation should add transaction/idempotency handling around payment activation and durable rate limiting around OTP requests.
