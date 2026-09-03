package com.greenink.api.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Repository
@Profile("!jooq")
public class ClasspathCatalogRepository implements CatalogRepository {
    private final List<UnitDefinition> units;
    private final Map<String, UnitDefinition> unitsById = new LinkedHashMap<>();
    private final Map<String, ChapterDefinition> chaptersById = new LinkedHashMap<>();
    private final Map<String, UnitDefinition> unitByChapterId = new LinkedHashMap<>();

    public ClasspathCatalogRepository(ObjectMapper mapper) throws IOException {
        CatalogFile file = mapper.readValue(new ClassPathResource("catalog.json").getInputStream(), CatalogFile.class);
        this.units = List.copyOf(file.units());
        for (UnitDefinition unit : units) {
            unitsById.put(unit.id(), unit);
            for (ChapterDefinition chapter : unit.chapters()) {
                chaptersById.put(chapter.id(), chapter);
                unitByChapterId.put(chapter.id(), unit);
            }
        }
    }

    @Override public List<UnitDefinition> findAllUnits() { return units; }
    @Override public Optional<UnitDefinition> findUnit(String unitId) { return Optional.ofNullable(unitsById.get(unitId)); }
    @Override public Optional<ChapterDefinition> findChapter(String chapterId) { return Optional.ofNullable(chaptersById.get(chapterId)); }
    @Override public Optional<UnitDefinition> findUnitByChapterId(String chapterId) { return Optional.ofNullable(unitByChapterId.get(chapterId)); }

    @Override
    public List<ChapterDefinition> searchChapters(String query, int limit) {
        String q = query.toLowerCase(Locale.ROOT).trim();
        List<ChapterDefinition> results = new ArrayList<>();
        for (ChapterDefinition chapter : chaptersById.values()) {
            if (chapter.title().toLowerCase(Locale.ROOT).contains(q)) {
                results.add(chapter);
                if (results.size() >= limit) break;
            }
        }
        return results;
    }

    @Override public int totalChapterCount() { return chaptersById.size(); }

    private record CatalogFile(List<UnitDefinition> units) {}
}
