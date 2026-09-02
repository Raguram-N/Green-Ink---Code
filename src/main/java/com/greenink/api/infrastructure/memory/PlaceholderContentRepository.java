package com.greenink.api.infrastructure.memory;

import com.greenink.api.content.ContentRepository;
import com.greenink.api.content.NoteDocument;
import org.springframework.core.io.ClassPathResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

/**
 * Review-stage fixture repository. It intentionally contains only two small HTML fixtures.
 * Do not migrate production Notes into this class.
 */
@Repository
@ConditionalOnProperty(name = "greenink.content.mode", havingValue = "placeholder", matchIfMissing = true)
public class PlaceholderContentRepository implements ContentRepository {
    private final Map<String, NoteDocument> documents;

    public PlaceholderContentRepository() throws IOException {
        documents = Map.of(
                "u1-c1", new NoteDocument("u1-c1", "dev-fixture-1", "HTML_FRAGMENT", read("dev-content/u1-c1.html")),
                "u1-c2", new NoteDocument("u1-c2", "dev-fixture-1", "HTML_FRAGMENT", read("dev-content/u1-c2.html"))
        );
    }

    @Override
    public Optional<NoteDocument> findNotesByChapterId(String chapterId, String language) {
        if ("ta".equalsIgnoreCase(language)) return Optional.empty();
        return Optional.ofNullable(documents.get(chapterId));
    }

    private String read(String path) throws IOException {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }
}
