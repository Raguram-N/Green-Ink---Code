package com.greenink.api.infrastructure.memory;

import com.greenink.api.pyq.PyqQuestion;
import com.greenink.api.pyq.PyqRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "greenink.content.mode", havingValue = "placeholder", matchIfMissing = true)
public class PlaceholderPyqRepository implements PyqRepository {
    private final Map<String, PyqQuestion> byId = new LinkedHashMap<>();

    public PlaceholderPyqRepository() {
        add(new PyqQuestion("q_u1c1_1", "u1-c1", "DEV FIXTURE: Scientific knowledge is primarily based on:",
                List.of(new PyqQuestion.Option("A", "Authority"), new PyqQuestion.Option("B", "Evidence"),
                        new PyqQuestion.Option("C", "Tradition"), new PyqQuestion.Option("D", "Belief")),
                "B", "Scientific knowledge is tested against evidence.", "DEV_FIXTURE"));
        add(new PyqQuestion("q_u1c1_2", "u1-c1", "DEV FIXTURE: A scientific claim should be:",
                List.of(new PyqQuestion.Option("A", "Unquestionable"), new PyqQuestion.Option("B", "Secret"),
                        new PyqQuestion.Option("C", "Testable"), new PyqQuestion.Option("D", "Traditional")),
                "C", "A scientific claim must be open to testing and verification.", "DEV_FIXTURE"));

        add(new PyqQuestion("q_u1c1_unkeyed", "u1-c1", "DEV FIXTURE: Unkeyed PYQ behavior check:",
                List.of(new PyqQuestion.Option("A", "Option A"), new PyqQuestion.Option("B", "Option B"),
                        new PyqQuestion.Option("C", "Option C"), new PyqQuestion.Option("D", "Option D")),
                null, null, "DEV_FIXTURE"));
        add(new PyqQuestion("q_u1c2_1", "u1-c2", "DEV FIXTURE: Reasoning from a general rule to a specific case is:",
                List.of(new PyqQuestion.Option("A", "Deduction"), new PyqQuestion.Option("B", "Guessing"),
                        new PyqQuestion.Option("C", "Memorisation"), new PyqQuestion.Option("D", "Imitation")),
                "A", "Deductive reasoning applies a general principle to a specific case.", "DEV_FIXTURE"));
    }

    private void add(PyqQuestion q) { byId.put(q.id(), q); }

    @Override
    public List<PyqQuestion> findByChapterId(String chapterId) {
        return byId.values().stream().filter(q -> q.chapterId().equals(chapterId)).toList();
    }

    @Override public Optional<PyqQuestion> findById(String questionId) { return Optional.ofNullable(byId.get(questionId)); }
    @Override public int totalQuestionCount() { return byId.size(); }
}
