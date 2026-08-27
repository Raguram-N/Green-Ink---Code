package com.greenink.api.search;

import java.util.List;

public interface SearchHistoryRepository {
    SearchHistoryItem add(String userId, String query);
    List<SearchHistoryItem> findRecent(String userId, int limit);
    void delete(String userId, String historyId);
    void deleteAll(String userId);
}
