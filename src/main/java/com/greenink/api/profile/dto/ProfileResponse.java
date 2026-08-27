package com.greenink.api.profile.dto;

import com.greenink.api.progress.dto.ProgressResponse;
import com.greenink.api.user.UserPreferences;

import java.time.Instant;
import java.util.Set;

public record ProfileResponse(
        String id,
        String identifier,
        Set<String> roles,
        boolean premium,
        String planCode,
        Instant premiumUntil,
        UserPreferences preferences,
        int notificationCount,
        ProgressResponse progress
) {}
