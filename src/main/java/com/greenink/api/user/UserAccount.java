package com.greenink.api.user;

import java.time.Instant;
import java.util.Set;

public record UserAccount(
        String id,
        String identifier,
        Set<String> roles,
        Instant createdAt,
        UserPreferences preferences
) {}
