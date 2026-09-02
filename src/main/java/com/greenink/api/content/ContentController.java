package com.greenink.api.content;

import com.greenink.api.content.dto.NoteContentResponse;
import com.greenink.api.security.SecurityUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${greenink.api.base-path}/chapters")
public class ContentController {
    private final ContentService contentService;

    public ContentController(ContentService contentService) { this.contentService = contentService; }

    @GetMapping(value = "/{chapterId}/notes", produces = "application/json;charset=UTF-8")
    public NoteContentResponse notes(@PathVariable String chapterId, @RequestParam(defaultValue = "en") String lang) {
        return contentService.getNotes(chapterId, SecurityUtil.currentUserId(), lang);
    }
}
