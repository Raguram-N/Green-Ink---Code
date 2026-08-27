package com.greenink.api.progress.dto;

import jakarta.validation.constraints.NotNull;

public record ChapterProgressRequest(@NotNull Boolean notesCompleted) {}
