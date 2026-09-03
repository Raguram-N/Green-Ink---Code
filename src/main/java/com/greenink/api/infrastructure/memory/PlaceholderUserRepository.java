package com.greenink.api.infrastructure.memory;

import com.greenink.api.user.UserAccount;
import com.greenink.api.user.UserPreferences;
import com.greenink.api.user.UserRepository;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Profile;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Review-stage placeholder. Replace with a Supabase/Postgres adapter after the schema is finalized.
 */
@Repository
@Profile("!jooq")
public class PlaceholderUserRepository implements UserRepository {
    private final ConcurrentHashMap<String, UserAccount> byId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> idByIdentifier = new ConcurrentHashMap<>();

    @Override
    public Optional<UserAccount> findById(String userId) {
        return Optional.ofNullable(byId.get(userId));
    }

    @Override
    public Optional<UserAccount> findByIdentifier(String normalizedIdentifier) {
        String id = idByIdentifier.get(normalizedIdentifier);
        return id == null ? Optional.empty() : findById(id);
    }

    @Override
    public synchronized UserAccount create(String normalizedIdentifier, Set<String> roles) {
        return findByIdentifier(normalizedIdentifier).orElseGet(() -> {
            UserAccount user = new UserAccount(
                    "usr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16),
                    normalizedIdentifier,
                    Set.copyOf(roles),
                    Instant.now(),
                    UserPreferences.defaults());
            byId.put(user.id(), user);
            idByIdentifier.put(user.identifier(), user.id());
            return user;
        });
    }

    @Override
    public UserAccount save(UserAccount user) {
        byId.put(user.id(), user);
        idByIdentifier.put(user.identifier(), user.id());
        return user;
    }

    @Override
    public void deleteById(String userId) {
        UserAccount removed = byId.remove(userId);
        if (removed != null) idByIdentifier.remove(removed.identifier());
    }
}
