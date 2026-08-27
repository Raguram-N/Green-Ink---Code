package com.greenink.api.catalog;

import java.util.List;
import java.util.Optional;

public interface CatalogRepository {
    List<UnitDefinition> findAllUnits();
    Optional<UnitDefinition> findUnit(String unitId);
    Optional<ChapterDefinition> findChapter(String chapterId);
    Optional<UnitDefinition> findUnitByChapterId(String chapterId);
    List<ChapterDefinition> searchChapters(String query, int limit);
    int totalChapterCount();
}
