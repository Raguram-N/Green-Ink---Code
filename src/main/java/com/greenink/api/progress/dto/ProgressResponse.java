package com.greenink.api.progress.dto;

import java.util.List;
import java.util.Set;

public record ProgressResponse(NotesProgress notes, PyqProgress pyq, List<UnitProgress> units) {
    public record NotesProgress(int completedChapters, int totalChapters, int percentage, Set<String> completedChapterIds) {}
    public record PyqProgress(int answered, int totalQuestions, int percentage, int bestCorrect, int chaptersStarted) {}
    public record UnitProgress(
            String unitId,
            String title,
            int notesCompleted,
            int notesTotal,
            int notesPercentage,
            int pyqAnswered,
            int pyqTotal,
            int pyqPercentage
    ) {}
}
