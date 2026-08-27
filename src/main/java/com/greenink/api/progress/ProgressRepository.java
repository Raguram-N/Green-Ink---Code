package com.greenink.api.progress;

import java.util.Map;
import java.util.Set;

public interface ProgressRepository {
    boolean isNotesCompleted(String userId, String chapterId);
    void setNotesCompleted(String userId, String chapterId, boolean completed);
    Set<String> notesCompletedChapterIds(String userId);
    void recordPyqAttempt(String userId, String chapterId, int answered, int correct);
    PyqProgressSnapshot pyqSnapshot(String userId);
    void resetNotes(String userId);
    void resetPyq(String userId);
    void deleteUser(String userId);

    record PyqChapterProgress(int answered, int bestCorrect) {}
    record PyqProgressSnapshot(Map<String, PyqChapterProgress> chapters) {
        public int answered() { return chapters.values().stream().mapToInt(PyqChapterProgress::answered).sum(); }
        public int bestCorrect() { return chapters.values().stream().mapToInt(PyqChapterProgress::bestCorrect).sum(); }
        public int chaptersStarted() { return (int) chapters.values().stream().filter(v -> v.answered() > 0).count(); }
    }
}
