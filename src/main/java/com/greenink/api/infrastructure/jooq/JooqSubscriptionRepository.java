package com.greenink.api.infrastructure.jooq;

import com.greenink.api.entitlement.Subscription;
import com.greenink.api.entitlement.SubscriptionRepository;
import org.jooq.DSLContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.greenink.jooq.generated.tables.SubscriptionPlans.SUBSCRIPTION_PLANS;
import static com.greenink.jooq.generated.tables.UserSubscriptions.USER_SUBSCRIPTIONS;
import static com.greenink.jooq.generated.tables.Users.USERS;

@Repository
@Profile("jooq")
public class JooqSubscriptionRepository
        implements SubscriptionRepository {

    private final DSLContext dsl;

    public JooqSubscriptionRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public Optional<Subscription> findByUserId(String userId) {
        Long databaseUserId = parseUserId(userId);

        if (databaseUserId == null) {
            return Optional.empty();
        }

        var record = dsl
                .select(
                        USER_SUBSCRIPTIONS.USER_ID,
                        SUBSCRIPTION_PLANS.CODE,
                        USER_SUBSCRIPTIONS.STATUS,
                        USER_SUBSCRIPTIONS.STARTS_AT,
                        USER_SUBSCRIPTIONS.EXPIRES_AT,
                        USER_SUBSCRIPTIONS.CREATED_AT
                )
                .from(USER_SUBSCRIPTIONS)
                .join(SUBSCRIPTION_PLANS)
                .on(
                        SUBSCRIPTION_PLANS.ID.eq(
                                USER_SUBSCRIPTIONS.PLAN_ID
                        )
                )
                .where(
                        USER_SUBSCRIPTIONS.USER_ID.eq(
                                databaseUserId
                        )
                )
                .orderBy(
                        USER_SUBSCRIPTIONS.EXPIRES_AT.desc(),
                        USER_SUBSCRIPTIONS.ID.desc()
                )
                .limit(1)
                .fetchOne();

        if (record == null) {
            return Optional.empty();
        }

        OffsetDateTime startsAt =
                record.get(USER_SUBSCRIPTIONS.STARTS_AT);

        OffsetDateTime expiresAt =
                record.get(USER_SUBSCRIPTIONS.EXPIRES_AT);

        OffsetDateTime createdAt =
                record.get(USER_SUBSCRIPTIONS.CREATED_AT);

        return Optional.of(
                new Subscription(
                        String.valueOf(
                                record.get(
                                        USER_SUBSCRIPTIONS.USER_ID
                                )
                        ),
                        record.get(SUBSCRIPTION_PLANS.CODE),
                        record.get(
                                USER_SUBSCRIPTIONS.STATUS
                        ),
                        startsAt == null
                                ? null
                                : startsAt.toInstant(),
                        expiresAt == null
                                ? null
                                : expiresAt.toInstant(),
                        createdAt == null
                                ? null
                                : createdAt.toInstant()
                )
        );
    }

    @Override
    public Subscription save(Subscription subscription) {
        Long databaseUserId =
                requireUserId(subscription.userId());

        var plan = dsl
                .select(
                        SUBSCRIPTION_PLANS.ID,
                        SUBSCRIPTION_PLANS.TIER_LEVEL
                )
                .from(SUBSCRIPTION_PLANS)
                .where(
                        SUBSCRIPTION_PLANS.CODE.eq(
                                subscription.planCode()
                        )
                )
                .and(SUBSCRIPTION_PLANS.IS_ACTIVE.eq(true))
                .fetchOne();

        if (plan == null) {
            throw new IllegalArgumentException(
                    "Unknown or inactive subscription plan: "
                            + subscription.planCode()
            );
        }

        Long planId =
                plan.get(SUBSCRIPTION_PLANS.ID);

        Integer tierLevel =
                plan.get(SUBSCRIPTION_PLANS.TIER_LEVEL);

        if ("ACTIVE".equals(subscription.status())) {
            dsl.update(USER_SUBSCRIPTIONS)
                    .set(
                            USER_SUBSCRIPTIONS.STATUS,
                            "UPGRADED"
                    )
                    .where(
                            USER_SUBSCRIPTIONS.USER_ID.eq(
                                    databaseUserId
                            )
                    )
                    .and(
                            USER_SUBSCRIPTIONS.STATUS.eq(
                                    "ACTIVE"
                            )
                    )
                    .execute();
        }

        OffsetDateTime startsAt =
                toOffsetDateTime(subscription.startedAt());

        OffsetDateTime expiresAt =
                toOffsetDateTime(subscription.expiresAt());

        if (startsAt == null || expiresAt == null) {
            throw new IllegalArgumentException(
                    "Subscription start and expiry are required."
            );
        }

        dsl.insertInto(USER_SUBSCRIPTIONS)
                .set(
                        USER_SUBSCRIPTIONS.USER_ID,
                        databaseUserId
                )
                .set(
                        USER_SUBSCRIPTIONS.PLAN_ID,
                        planId
                )
                .set(
                        USER_SUBSCRIPTIONS.STARTS_AT,
                        startsAt
                )
                .set(
                        USER_SUBSCRIPTIONS.EXPIRES_AT,
                        expiresAt
                )
                .set(
                        USER_SUBSCRIPTIONS.STATUS,
                        subscription.status()
                )
                .execute();

        if ("ACTIVE".equals(subscription.status())) {
            dsl.update(USERS)
                    .set(
                            USERS.CACHED_TIER_LEVEL,
                            tierLevel == null ? 10 : tierLevel
                    )
                    .set(
                            USERS.TIER_EXPIRES_AT,
                            expiresAt
                    )
                    .where(USERS.ID.eq(databaseUserId))
                    .execute();
        }

        return findByUserId(subscription.userId())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Subscription disappeared after save."
                        )
                );
    }

    @Override
    public void deleteByUserId(String userId) {
        Long databaseUserId = parseUserId(userId);

        if (databaseUserId == null) {
            return;
        }

        dsl.deleteFrom(USER_SUBSCRIPTIONS)
                .where(
                        USER_SUBSCRIPTIONS.USER_ID.eq(
                                databaseUserId
                        )
                )
                .execute();

        dsl.update(USERS)
                .set(USERS.CACHED_TIER_LEVEL, 0)
                .set(
                        USERS.TIER_EXPIRES_AT,
                        (OffsetDateTime) null
                )
                .where(USERS.ID.eq(databaseUserId))
                .execute();
    }

    private OffsetDateTime toOffsetDateTime(Instant value) {
        return value == null
                ? null
                : value.atOffset(ZoneOffset.UTC);
    }

    private Long parseUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }

        try {
            return Long.valueOf(userId);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long requireUserId(String userId) {
        Long id = parseUserId(userId);

        if (id == null) {
            throw new IllegalArgumentException(
                    "Invalid database user ID: " + userId
            );
        }

        return id;
    }
}