package com.greenink.api.search;

import com.greenink.api.search.dto.SearchResponse;
import com.greenink.api.security.SecurityUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class SearchController {
    private final SearchService searchService;
    private final SearchHistoryRepository historyRepository;

    public SearchController(SearchService searchService, SearchHistoryRepository historyRepository) {
        this.searchService = searchService;
        this.historyRepository = historyRepository;
    }

    @GetMapping("/search")
    public SearchResponse search(@RequestParam String q, @RequestParam(defaultValue = "20") int limit) {
        return searchService.search(q, limit, SecurityUtil.currentUserId());
    }

    @GetMapping("/me/search-history")
    public List<SearchHistoryItem> history(@RequestParam(defaultValue = "10") int limit) {
        return historyRepository.findRecent(SecurityUtil.requireUserId(), Math.max(1, Math.min(limit, 30)));
    }

    @DeleteMapping("/me/search-history/{historyId}")
    public ResponseEntity<Void> deleteHistoryItem(@PathVariable String historyId) {
        historyRepository.delete(SecurityUtil.requireUserId(), historyId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me/search-history")
    public ResponseEntity<Void> clearHistory() {
        historyRepository.deleteAll(SecurityUtil.requireUserId());
        return ResponseEntity.noContent().build();
    }
}
