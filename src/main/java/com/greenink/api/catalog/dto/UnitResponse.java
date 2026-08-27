package com.greenink.api.catalog.dto;

import java.util.List;

public record UnitResponse(String id, int number, String roman, String title, List<ChapterResponse> chapters) {}
