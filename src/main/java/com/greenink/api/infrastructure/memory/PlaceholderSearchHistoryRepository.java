package com.greenink.api.infrastructure.memory;

import com.greenink.api.search.SearchHistoryItem;
import com.greenink.api.search.SearchHistoryRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class PlaceholderSearchHistoryRepository implements SearchHistoryRepository {
    private final ConcurrentHashMap<String, List<SearchHistoryItem>> items = new ConcurrentHashMap<>();

    @Override
    public SearchHistoryItem add(String userId, String query) {
        SearchHistoryItem item = new SearchHistoryItem("sh_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12), query, Instant.now());
        List<SearchHistoryItem> list = items.computeIfAbsent(userId, ignored -> java.util.Collections.synchronizedList(new ArrayList<>()));
        synchronized (list) {
            list.removeIf(existing -> existing.query().equalsIgnoreCase(query));
            list.add(item);
            while (list.size() > 30) list.remove(0);
        }
        return item;
    }

    @Override
    public List<SearchHistoryItem> findRecent(String userId, int limit) {
        List<SearchHistoryItem> list = items.getOrDefault(userId, List.of());
        synchronized (list) {
            return list.stream().sorted(Comparator.comparing(SearchHistoryItem::searchedAt).reversed()).limit(limit).toList();
        }
    }

    @Override
    public void delete(String userId, String historyId) {
        List<SearchHistoryItem> list = items.get(userId);
        if (list != null) synchronized (list) { list.removeIf(item -> item.id().equals(historyId)); }
    }

    @Override public void deleteAll(String userId) { items.remove(userId); }
}
