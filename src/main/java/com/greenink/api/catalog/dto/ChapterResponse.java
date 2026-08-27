package com.greenink.api.catalog.dto;

import com.greenink.api.catalog.AccessTier;

public record ChapterResponse(
        String id,
        int number,
        String title,
        AccessTier access,
        boolean hasNotes,
        Boolean completed,
        boolean accessible
) {}
