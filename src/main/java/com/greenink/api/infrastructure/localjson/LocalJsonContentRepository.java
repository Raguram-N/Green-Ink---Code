package com.greenink.api.infrastructure.localjson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenink.api.config.GreenInkProperties;
import com.greenink.api.content.ContentRepository;
import com.greenink.api.content.NoteDocument;
import com.greenink.api.pyq.PyqQuestion;
import com.greenink.api.pyq.PyqRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Repository
@ConditionalOnProperty(name = "greenink.content.mode", havingValue = "local-json")
public class LocalJsonContentRepository implements ContentRepository, PyqRepository {
    private final Map<String, NoteDocument> notesByChapter = new LinkedHashMap<>();
    private final Map<String, NoteDocument> tamilNotesByChapter = new LinkedHashMap<>();
    private final Map<String, PyqQuestion> questionsById = new LinkedHashMap<>();
    private final Map<String, Map<String, List<String>>> kuralTextByChapter = new LinkedHashMap<>();

    public LocalJsonContentRepository(ObjectMapper mapper, GreenInkProperties properties) throws IOException {
        String configuredPath = properties.content().localPath();
        if (configuredPath == null || configuredPath.isBlank()) {
            throw new IllegalStateException("GREEN_INK_CONTENT_LOCAL_PATH is required when content mode is local-json");
        }
        Path root = Path.of(configuredPath);
        if (!Files.isDirectory(root)) {
            throw new IllegalStateException("Local content directory not found: " + root);
        }

        List<Path> files;
        try (Stream<Path> stream = Files.list(root)) {
            files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().matches("u[1-6]-c\\d+\\.json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }

        for (Path file : files) loadFile(mapper, file);

        Path tamilRoot = root.resolve("ta");
        if (Files.isDirectory(tamilRoot)) {
            try (Stream<Path> stream = Files.list(tamilRoot)) {
                for (Path file : stream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().matches("u[1-6]-c\\d+\\.html"))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .toList()) {
                    String chapterId = file.getFileName().toString().replaceFirst("\\.html$", "");
                    String notes = Files.readString(file);
                    tamilNotesByChapter.put(chapterId,
                            new NoteDocument(chapterId, "local-json-ta-1", "HTML_FRAGMENT", notes));
                }
            }
        }

        if (notesByChapter.size() != 200) {
            throw new IllegalStateException("Expected 200 chapter files, loaded " + notesByChapter.size());
        }
    }

    private void loadFile(ObjectMapper mapper, Path file) throws IOException {
        JsonNode root = mapper.readTree(file.toFile());
        int unit = root.path("unit").asInt();
        int chapter = root.path("chapter").asInt();
        String chapterId = "u" + (unit + 1) + "-c" + (chapter + 1);

        String notes = root.path("notes").asText("");
        notesByChapter.put(chapterId, new NoteDocument(chapterId, "local-json-1", "HTML_FRAGMENT", notes));

        Map<String, List<String>> chapterKuralText = new LinkedHashMap<>();
        JsonNode kuralTextNode = root.path("kuralText");
        if (kuralTextNode.isObject()) {
            kuralTextNode.fields().forEachRemaining(entry -> {
                List<String> lines = new ArrayList<>();
                if (entry.getValue().isArray()) entry.getValue().forEach(line -> lines.add(line.asText()));
                chapterKuralText.put(entry.getKey(), List.copyOf(lines));
            });
        }
        kuralTextByChapter.put(chapterId, Map.copyOf(chapterKuralText));

        JsonNode pyq = root.path("pyq");
        if (!pyq.isArray()) return;

        int index = 0;
        for (JsonNode q : pyq) {
            index++;
            String id = "q_" + chapterId.replace("-", "") + "_" + index;

            List<PyqQuestion.Option> options = new ArrayList<>();
            JsonNode optionNode = q.path("o");
            if (optionNode.isArray()) {
                for (int i = 0; i < optionNode.size(); i++) {
                    String key = String.valueOf((char) ('A' + i));
                    options.add(new PyqQuestion.Option(key, optionNode.get(i).asText()));
                }
            }

            String correctOption = q.hasNonNull("a") && !q.path("a").asText().isBlank() ? q.path("a").asText() : null;
            String source = q.hasNonNull("s") ? q.path("s").asText() : null;
            String unkeyedStatus = q.hasNonNull("u") ? q.path("u").asText() : null;

            List<List<String>> matchRows = new ArrayList<>();
            JsonNode matchNode = q.path("m");
            if (matchNode.isArray()) {
                for (JsonNode row : matchNode) {
                    if (row.isArray()) {
                        List<String> cells = new ArrayList<>();
                        row.forEach(cell -> cells.add(cell.asText()));
                        matchRows.add(cells);
                    }
                }
            }

            List<Integer> kuralRefs = new ArrayList<>();
            JsonNode kuralNode = q.path("k");
            if (kuralNode.isArray()) kuralNode.forEach(v -> kuralRefs.add(v.asInt()));

            Map<String,Object> metadata = new LinkedHashMap<>();
            for (String key : List.of("n","p","srcnum")) {
                if (q.has(key)) metadata.put(key, mapper.convertValue(q.get(key), Object.class));
            }

            PyqQuestion question = new PyqQuestion(
                    id, chapterId, q.path("q").asText(), options, correctOption, null, source,
                    matchRows, unkeyedStatus, kuralRefs, metadata);
            questionsById.put(id, question);
        }
    }

    @Override
    public Optional<NoteDocument> findNotesByChapterId(String chapterId, String language) {
        return Optional.ofNullable("ta".equalsIgnoreCase(language) ? tamilNotesByChapter.get(chapterId) : notesByChapter.get(chapterId));
    }

    @Override
    public List<PyqQuestion> findByChapterId(String chapterId) {
        return questionsById.values().stream().filter(q -> q.chapterId().equals(chapterId)).toList();
    }

    @Override
    public Optional<PyqQuestion> findById(String questionId) {
        return Optional.ofNullable(questionsById.get(questionId));
    }

    @Override
    public Map<String, List<String>> kuralTextByChapterId(String chapterId) {
        return kuralTextByChapter.getOrDefault(chapterId, Map.of());
    }

    @Override
    public int totalQuestionCount() {
        return questionsById.size();
    }
}