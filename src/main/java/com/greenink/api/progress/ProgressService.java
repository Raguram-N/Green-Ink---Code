package com.greenink.api.progress;

import com.greenink.api.catalog.CatalogRepository;
import com.greenink.api.catalog.UnitDefinition;
import com.greenink.api.common.NotFoundException;
import com.greenink.api.progress.dto.ChapterProgressResponse;
import com.greenink.api.progress.dto.ProgressResponse;
import com.greenink.api.pyq.PyqMetadata;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

@Service
public class ProgressService {
    private final ProgressRepository progressRepository;
    private final CatalogRepository catalogRepository;
    private final PyqMetadata pyqMetadata;

    public ProgressService(ProgressRepository progressRepository, CatalogRepository catalogRepository, PyqMetadata pyqMetadata) {
        this.progressRepository = progressRepository;
        this.catalogRepository = catalogRepository;
        this.pyqMetadata = pyqMetadata;
    }

    public ChapterProgressResponse setNotesCompleted(String userId, String chapterId, boolean completed) {
        catalogRepository.findChapter(chapterId)
                .orElseThrow(() -> new NotFoundException("CHAPTER_NOT_FOUND", "Chapter not found."));
        progressRepository.setNotesCompleted(userId, chapterId, completed);
        return new ChapterProgressResponse(chapterId, completed, Instant.now());
    }

    public ProgressResponse getProgress(String userId) {
        Set<String> completed = progressRepository.notesCompletedChapterIds(userId);
        int totalNotes = catalogRepository.totalChapterCount();
        var pyq = progressRepository.pyqSnapshot(userId);
        var unitRows = catalogRepository.findAllUnits().stream().map(unit -> unitProgress(unit, completed, pyq)).toList();
        return new ProgressResponse(
                new ProgressResponse.NotesProgress(completed.size(), totalNotes, percentage(completed.size(), totalNotes), Set.copyOf(completed)),
                new ProgressResponse.PyqProgress(pyq.answered(), pyqMetadata.totalQuestions(),
                        percentage(pyq.answered(), pyqMetadata.totalQuestions()), pyq.bestCorrect(), pyq.chaptersStarted()),
                unitRows);
    }

    public void recordPyqAttempt(String userId, String chapterId, int answered, int correct) {
        progressRepository.recordPyqAttempt(userId, chapterId, answered, correct);
    }

    public void resetNotes(String userId) { progressRepository.resetNotes(userId); }
    public void resetPyq(String userId) { progressRepository.resetPyq(userId); }

    private ProgressResponse.UnitProgress unitProgress(UnitDefinition unit, Set<String> completed, ProgressRepository.PyqProgressSnapshot pyq) {
        int notesDone = (int) unit.chapters().stream().filter(c -> completed.contains(c.id())).count();
        int pyqAnswered = unit.chapters().stream()
                .map(c -> pyq.chapters().get(c.id()))
                .filter(java.util.Objects::nonNull)
                .mapToInt(ProgressRepository.PyqChapterProgress::answered)
                .sum();
        int pyqTotal = pyqMetadata.totalForUnit(unit.id());
        return new ProgressResponse.UnitProgress(unit.id(), unit.title(), notesDone, unit.chapters().size(),
                percentage(notesDone, unit.chapters().size()), pyqAnswered, pyqTotal, percentage(pyqAnswered, pyqTotal));
    }

    private int percentage(int value, int total) {
        return total == 0 ? 0 : (int) Math.round(value * 100.0 / total);
    }
}
