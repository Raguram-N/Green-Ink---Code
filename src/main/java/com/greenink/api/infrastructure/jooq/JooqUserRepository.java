package com.greenink.api.infrastructure.jooq;

import com.greenink.api.user.UserAccount;
import com.greenink.api.user.UserPreferences;
import com.greenink.api.user.UserRepository;
import com.greenink.jooq.generated.tables.records.UsersRecord;
import org.jooq.DSLContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.greenink.jooq.generated.tables.Users.USERS;

@Repository
@Profile("jooq")
public class JooqUserRepository implements UserRepository {

    private final DSLContext dsl;

    /*
     * The locked production schema does not contain roles/preferences.
     * Keep these application-level values in memory for now rather than
     * introducing unsupported database columns.
     */
    private final ConcurrentHashMap<Long, Set<String>> rolesByUserId =
            new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Long, UserPreferences> preferencesByUserId =
            new ConcurrentHashMap<>();

    public JooqUserRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public Optional<UserAccount> findById(String userId) {
        Long id = parseUserId(userId);

        if (id == null) {
            return Optional.empty();
        }

        UsersRecord record = dsl
                .selectFrom(USERS)
                .where(USERS.ID.eq(id))
                .and(USERS.STATUS.eq("ACTIVE"))
                .fetchOne();

        return Optional.ofNullable(record)
                .map(this::toDomain);
    }

    @Override
    public Optional<UserAccount> findByIdentifier(
            String normalizedIdentifier
    ) {
        if (normalizedIdentifier == null
                || normalizedIdentifier.isBlank()) {
            return Optional.empty();
        }

        UsersRecord record;

        if (isEmail(normalizedIdentifier)) {
            record = dsl
                    .selectFrom(USERS)
                    .where(
                            USERS.EMAIL.eq(
                                    normalizedIdentifier.toLowerCase()
                            )
                    )
                    .and(USERS.STATUS.eq("ACTIVE"))
                    .fetchOne();
        } else {
            record = dsl
                    .selectFrom(USERS)
                    .where(
                            USERS.PHONE_NUMBER.eq(
                                    normalizedIdentifier
                            )
                    )
                    .and(USERS.STATUS.eq("ACTIVE"))
                    .fetchOne();
        }

        return Optional.ofNullable(record)
                .map(this::toDomain);
    }

    @Override
    public UserAccount create(
            String normalizedIdentifier,
            Set<String> roles
    ) {
        Optional<UserAccount> existing =
                findByIdentifier(normalizedIdentifier);

        if (existing.isPresent()) {
            return existing.get();
        }

        var insert = dsl.insertInto(USERS)
                .set(USERS.STATUS, "ACTIVE");

        if (isEmail(normalizedIdentifier)) {
            insert.set(
                    USERS.EMAIL,
                    normalizedIdentifier.toLowerCase()
            );
        } else {
            insert.set(
                    USERS.PHONE_NUMBER,
                    normalizedIdentifier
            );
        }

        UsersRecord created = insert
                .returning()
                .fetchOne();

        if (created == null) {
            throw new IllegalStateException(
                    "Failed to create user."
            );
        }

        Long id = created.get(USERS.ID);

        rolesByUserId.put(
                id,
                roles == null
                        ? Set.of("ROLE_USER")
                        : Set.copyOf(roles)
        );

        preferencesByUserId.put(
                id,
                UserPreferences.defaults()
        );

        return toDomain(created);
    }

    @Override
    public UserAccount save(UserAccount user) {
        Long id = parseUserId(user.id());

        if (id == null) {
            throw new IllegalArgumentException(
                    "Invalid database user ID: " + user.id()
            );
        }

        boolean exists = dsl.fetchExists(
                dsl.selectOne()
                        .from(USERS)
                        .where(USERS.ID.eq(id))
                        .and(USERS.STATUS.eq("ACTIVE"))
        );

        if (!exists) {
            throw new IllegalArgumentException(
                    "User does not exist: " + user.id()
            );
        }

        rolesByUserId.put(
                id,
                user.roles() == null
                        ? Set.of("ROLE_USER")
                        : Set.copyOf(user.roles())
        );

        preferencesByUserId.put(
                id,
                user.preferences() == null
                        ? UserPreferences.defaults()
                        : user.preferences()
        );

        return findById(user.id())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "User disappeared after save."
                        )
                );
    }

    @Override
    public void deleteById(String userId) {
        Long id = parseUserId(userId);

        if (id == null) {
            return;
        }

        dsl.deleteFrom(USERS)
                .where(USERS.ID.eq(id))
                .execute();

        rolesByUserId.remove(id);
        preferencesByUserId.remove(id);
    }

    private UserAccount toDomain(UsersRecord record) {
        Long id = record.get(USERS.ID);

        String identifier =
                record.get(USERS.EMAIL) != null
                        ? record.get(USERS.EMAIL)
                        : record.get(USERS.PHONE_NUMBER);

        OffsetDateTime createdAt =
                record.get(USERS.CREATED_AT);

        Instant createdInstant =
                createdAt == null
                        ? Instant.now()
                        : createdAt.toInstant();

        Set<String> roles =
                rolesByUserId.getOrDefault(
                        id,
                        Set.of("ROLE_USER")
                );

        UserPreferences preferences =
                preferencesByUserId.getOrDefault(
                        id,
                        UserPreferences.defaults()
                );

        return new UserAccount(
                String.valueOf(id),
                identifier,
                roles,
                createdInstant,
                preferences
        );
    }

    private boolean isEmail(String identifier) {
        return identifier != null
                && identifier.contains("@");
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
}