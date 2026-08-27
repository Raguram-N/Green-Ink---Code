package com.greenink.api.progress.dto;

import java.time.Instant;

public record ChapterProgressResponse(String chapterId, boolean notesCompleted, Instant updatedAt) {}
