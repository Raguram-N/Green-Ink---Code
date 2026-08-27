package com.greenink.api.catalog;

import com.greenink.api.catalog.dto.ChapterResponse;
import com.greenink.api.catalog.dto.UnitResponse;
import com.greenink.api.security.SecurityUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {
    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) { this.catalogService = catalogService; }

    @GetMapping("/units")
    public List<UnitResponse> units() { return catalogService.getUnits(SecurityUtil.currentUserId()); }

    @GetMapping("/units/{unitId}")
    public UnitResponse unit(@PathVariable String unitId) { return catalogService.getUnit(unitId, SecurityUtil.currentUserId()); }

    @GetMapping("/units/{unitId}/chapters")
    public List<ChapterResponse> chapters(@PathVariable String unitId) {
        return catalogService.getUnit(unitId, SecurityUtil.currentUserId()).chapters();
    }

    @GetMapping("/chapters/{chapterId}")
    public ChapterResponse chapter(@PathVariable String chapterId) {
        return catalogService.getChapter(chapterId, SecurityUtil.currentUserId());
    }
}
