package com.greenink.api.pyq;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PyqRepository {
    List<PyqQuestion> findByChapterId(String chapterId);
    Optional<PyqQuestion> findById(String questionId);
    int totalQuestionCount();
    default Map<String, List<String>> kuralTextByChapterId(String chapterId) { return Map.of(); }
}
