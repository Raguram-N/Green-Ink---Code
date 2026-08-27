package com.greenink.api.content;

import java.util.Optional;

/**
 * Schema-independent content contract for the review stage.
 * The first production adapter can map this to Supabase/Postgres once the schema is finalized.
 */
public interface ContentRepository {
    Optional<NoteDocument> findNotesByChapterId(String chapterId);
}
