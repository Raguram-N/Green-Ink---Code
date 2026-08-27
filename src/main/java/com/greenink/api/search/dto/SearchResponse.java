package com.greenink.api.search.dto;

import java.util.List;

public record SearchResponse(String query, int count, List<SearchResultResponse> results) {}
