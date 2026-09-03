package com.greenink.api.infrastructure.memory;

import com.greenink.api.progress.ProgressRepository;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Profile;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("!jooq")
public class PlaceholderProgressRepository implements ProgressRepository {
    private final ConcurrentHashMap<String, Set<String>> notes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, PyqChapterProgress>> pyq = new ConcurrentHashMap<>();

    @Override
    public boolean isNotesCompleted(String userId, String chapterId) {
        return notes.getOrDefault(userId, Set.of()).contains(chapterId);
    }

    @Override
    public void setNotesCompleted(String userId, String chapterId, boolean completed) {
        Set<String> set = notes.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet());
        if (completed) set.add(chapterId); else set.remove(chapterId);
    }

    @Override
    public Set<String> notesCompletedChapterIds(String userId) {
        return Set.copyOf(notes.getOrDefault(userId, Set.of()));
    }

    @Override
    public void recordPyqAttempt(String userId, String chapterId, int answered, int correct) {
        var byChapter = pyq.computeIfAbsent(userId, ignored -> new ConcurrentHashMap<>());
        byChapter.merge(chapterId, new PyqChapterProgress(answered, correct),
                (oldValue, newValue) -> new PyqChapterProgress(
                        Math.max(oldValue.answered(), newValue.answered()),
                        Math.max(oldValue.bestCorrect(), newValue.bestCorrect())));
    }

    @Override
    public PyqProgressSnapshot pyqSnapshot(String userId) {
        Map<String, PyqChapterProgress> map = pyq.get(userId);
        return new PyqProgressSnapshot(map == null ? Map.of() : Map.copyOf(map));
    }

    @Override public void resetNotes(String userId) { notes.remove(userId); }
    @Override public void resetPyq(String userId) { pyq.remove(userId); }
    @Override public void deleteUser(String userId) { resetNotes(userId); resetPyq(userId); }
}
