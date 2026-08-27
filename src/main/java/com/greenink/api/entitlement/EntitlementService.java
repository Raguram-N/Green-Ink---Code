package com.greenink.api.entitlement;

import com.greenink.api.catalog.AccessTier;
import com.greenink.api.catalog.ChapterDefinition;
import com.greenink.api.common.PremiumRequiredException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class EntitlementService {
    private final SubscriptionRepository subscriptionRepository;

    public EntitlementService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    public boolean hasActivePremium(String userId) {
        return subscriptionRepository.findByUserId(userId)
                .map(s -> s.activeAt(Instant.now()))
                .orElse(false);
    }

    public void requireChapterAccess(ChapterDefinition chapter, Optional<String> userId) {
        if (chapter.access() == AccessTier.FREE) return;
        if (userId.isPresent() && hasActivePremium(userId.get())) return;
        throw new PremiumRequiredException("This chapter requires Green Ink Premium.");
    }
}
