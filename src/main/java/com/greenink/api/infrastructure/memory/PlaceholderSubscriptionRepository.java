package com.greenink.api.infrastructure.memory;

import com.greenink.api.entitlement.Subscription;
import com.greenink.api.entitlement.SubscriptionRepository;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Profile;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("!jooq")
public class PlaceholderSubscriptionRepository implements SubscriptionRepository {
    private final ConcurrentHashMap<String, Subscription> subscriptions = new ConcurrentHashMap<>();
    @Override public Optional<Subscription> findByUserId(String userId) { return Optional.ofNullable(subscriptions.get(userId)); }
    @Override public Subscription save(Subscription subscription) { subscriptions.put(subscription.userId(), subscription); return subscription; }
    @Override public void deleteByUserId(String userId) { subscriptions.remove(userId); }
}
