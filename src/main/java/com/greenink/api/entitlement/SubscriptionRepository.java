package com.greenink.api.entitlement;

import java.util.Optional;

public interface SubscriptionRepository {
    Optional<Subscription> findByUserId(String userId);
    Subscription save(Subscription subscription);
    void deleteByUserId(String userId);
}
