package com.greenink.api.search.dto;

import com.greenink.api.catalog.AccessTier;

public record SearchResultResponse(
        String chapterId,
        String chapterTitle,
        String unitId,
        String unitTitle,
        AccessTier access,
        boolean accessible
) {}
