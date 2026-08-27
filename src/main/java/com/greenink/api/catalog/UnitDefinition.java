package com.greenink.api.catalog;

import java.util.List;

public record UnitDefinition(String id, int number, String roman, String title, List<ChapterDefinition> chapters) {}
