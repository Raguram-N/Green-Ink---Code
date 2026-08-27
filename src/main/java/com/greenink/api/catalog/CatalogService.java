package com.greenink.api.catalog;

import com.greenink.api.catalog.dto.ChapterResponse;
import com.greenink.api.catalog.dto.UnitResponse;
import com.greenink.api.common.NotFoundException;
import com.greenink.api.entitlement.EntitlementService;
import com.greenink.api.progress.ProgressRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CatalogService {
    private final CatalogRepository catalogRepository;
    private final ProgressRepository progressRepository;
    private final EntitlementService entitlementService;

    public CatalogService(CatalogRepository catalogRepository, ProgressRepository progressRepository, EntitlementService entitlementService) {
        this.catalogRepository = catalogRepository;
        this.progressRepository = progressRepository;
        this.entitlementService = entitlementService;
    }

    public List<UnitResponse> getUnits(Optional<String> userId) {
        return catalogRepository.findAllUnits().stream().map(unit -> toResponse(unit, userId)).toList();
    }

    public UnitResponse getUnit(String unitId, Optional<String> userId) {
        UnitDefinition unit = catalogRepository.findUnit(unitId)
                .orElseThrow(() -> new NotFoundException("UNIT_NOT_FOUND", "Unit not found."));
        return toResponse(unit, userId);
    }

    public ChapterResponse getChapter(String chapterId, Optional<String> userId) {
        ChapterDefinition chapter = catalogRepository.findChapter(chapterId)
                .orElseThrow(() -> new NotFoundException("CHAPTER_NOT_FOUND", "Chapter not found."));
        return toChapterResponse(chapter, userId);
    }

    public ChapterDefinition requireChapter(String chapterId) {
        return catalogRepository.findChapter(chapterId)
                .orElseThrow(() -> new NotFoundException("CHAPTER_NOT_FOUND", "Chapter not found."));
    }

    private UnitResponse toResponse(UnitDefinition unit, Optional<String> userId) {
        return new UnitResponse(unit.id(), unit.number(), unit.roman(), unit.title(),
                unit.chapters().stream().map(c -> toChapterResponse(c, userId)).toList());
    }

    private ChapterResponse toChapterResponse(ChapterDefinition chapter, Optional<String> userId) {
        Boolean completed = userId.map(id -> progressRepository.isNotesCompleted(id, chapter.id())).orElse(null);
        boolean accessible = chapter.access() == AccessTier.FREE || userId.map(entitlementService::hasActivePremium).orElse(false);
        return new ChapterResponse(chapter.id(), chapter.number(), chapter.title(), chapter.access(), chapter.hasNotes(), completed, accessible);
    }
}
