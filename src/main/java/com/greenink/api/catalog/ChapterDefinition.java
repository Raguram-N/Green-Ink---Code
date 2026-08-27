package com.greenink.api.catalog;

public record ChapterDefinition(
        String id,
        int number,
        String title,
        AccessTier access,
        boolean hasNotes
) {}
