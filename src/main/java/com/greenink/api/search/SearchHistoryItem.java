package com.greenink.api.search;

import java.time.Instant;

public record SearchHistoryItem(String id, String query, Instant searchedAt) {}
