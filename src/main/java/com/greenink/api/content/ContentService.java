package com.greenink.api.content;

import com.greenink.api.catalog.CatalogService;
import com.greenink.api.catalog.ChapterDefinition;
import com.greenink.api.common.NotFoundException;
import com.greenink.api.content.dto.NoteContentResponse;
import com.greenink.api.entitlement.EntitlementService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ContentService {
    private final CatalogService catalogService;
    private final EntitlementService entitlementService;
    private final ContentRepository contentRepository;

    public ContentService(CatalogService catalogService, EntitlementService entitlementService, ContentRepository contentRepository) {
        this.catalogService = catalogService;
        this.entitlementService = entitlementService;
        this.contentRepository = contentRepository;
    }

    public NoteContentResponse getNotes(String chapterId, Optional<String> userId) {
        ChapterDefinition chapter = catalogService.requireChapter(chapterId);
        entitlementService.requireChapterAccess(chapter, userId);
        NoteDocument document = contentRepository.findNotesByChapterId(chapterId)
                .orElseThrow(() -> new NotFoundException("CONTENT_NOT_MIGRATED",
                        "Notes are not present in the placeholder repository for this chapter yet."));
        return new NoteContentResponse(document.chapterId(), document.version(), document.format(), document.bodyHtml());
    }
}
