package com.greenink.api.search;

import com.greenink.api.catalog.AccessTier;
import com.greenink.api.catalog.CatalogRepository;
import com.greenink.api.common.BadRequestException;
import com.greenink.api.entitlement.EntitlementService;
import com.greenink.api.search.dto.SearchResponse;
import com.greenink.api.search.dto.SearchResultResponse;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SearchService {
    private final CatalogRepository catalogRepository;
    private final EntitlementService entitlementService;

    public SearchService(CatalogRepository catalogRepository, EntitlementService entitlementService) {
        this.catalogRepository = catalogRepository;
        this.entitlementService = entitlementService;
    }

    public SearchResponse search(String rawQuery, int limit, Optional<String> userId) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.length() < 2) throw new BadRequestException("SEARCH_QUERY_TOO_SHORT", "Search query must contain at least 2 characters.");
        int safeLimit = Math.max(1, Math.min(limit, 80));
        var results = catalogRepository.searchChapters(query, safeLimit).stream().map(chapter -> {
            var unit = catalogRepository.findUnitByChapterId(chapter.id()).orElseThrow();
            boolean accessible = chapter.access() == AccessTier.FREE || userId.map(entitlementService::hasActivePremium).orElse(false);
            return new SearchResultResponse(chapter.id(), chapter.title(), unit.id(), unit.title(), chapter.access(), accessible);
        }).toList();
        return new SearchResponse(query, results.size(), results);
    }
}
