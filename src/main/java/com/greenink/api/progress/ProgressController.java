package com.greenink.api.progress;

import com.greenink.api.progress.dto.ChapterProgressRequest;
import com.greenink.api.progress.dto.ChapterProgressResponse;
import com.greenink.api.progress.dto.ProgressResponse;
import com.greenink.api.security.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${greenink.api.base-path}/me/progress")
public class ProgressController {
    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) { this.progressService = progressService; }

    @GetMapping
    public ProgressResponse getProgress() { return progressService.getProgress(SecurityUtil.requireUserId()); }

    @PutMapping("/chapters/{chapterId}")
    public ChapterProgressResponse updateChapter(@PathVariable String chapterId, @Valid @RequestBody ChapterProgressRequest request) {
        return progressService.setNotesCompleted(SecurityUtil.requireUserId(), chapterId, request.notesCompleted());
    }

    @DeleteMapping("/notes")
    public ResponseEntity<Void> resetNotes() {
        progressService.resetNotes(SecurityUtil.requireUserId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/pyq")
    public ResponseEntity<Void> resetPyq() {
        progressService.resetPyq(SecurityUtil.requireUserId());
        return ResponseEntity.noContent().build();
    }
}
