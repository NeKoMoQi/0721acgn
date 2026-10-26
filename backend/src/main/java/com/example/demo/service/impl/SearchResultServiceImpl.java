package com.example.demo.service.impl;

import com.example.demo.com.SearchResult;
import com.example.demo.mapper.mysql.SearchResultMapper;
import com.example.demo.service.SearchResultService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class SearchResultServiceImpl implements SearchResultService {
    private final SearchResultMapper searchResultMapper;

    public SearchResultServiceImpl(SearchResultMapper searchResultMapper) {
        this.searchResultMapper = searchResultMapper;
    }

    @Override
    public List<SearchResult> getSearchResults(String search) {
        return searchResultMapper.getSearchResult(search);
    }

    @Override
    public List<SearchResult> getSearchResultsAdvanced(
            String search,
            String genre,
            String status,
            Integer yearMin,
            Integer yearMax,
            String sortBy,
            String mediaType,
            Integer limit) {
        boolean hasFilters =
                (genre != null && !genre.isBlank())
                        || (status != null && !status.isBlank())
                        || yearMin != null
                        || yearMax != null
                        || (mediaType != null && !mediaType.isBlank())
                        || (sortBy != null && !sortBy.isBlank() && !"default".equalsIgnoreCase(sortBy.trim()))
                        || limit != null;

        String sTrim = search == null ? "" : search.trim();
        if (!hasFilters && !sTrim.isEmpty()) {
            return getSearchResults(search);
        }

        String g = emptyToNull(genre);
        String st = emptyToNull(status);
        String normalizedSort = normalizeSort(sortBy);
        String mtRaw = mediaType == null ? "" : mediaType.trim().toLowerCase();
        int lim = limit == null ? 120 : Math.min(300, Math.max(1, limit));

        List<SearchResult> merged = new ArrayList<>();
        if (mtRaw.isEmpty()) {
            merged.addAll(searchResultMapper.searchComicsGlobal(sTrim, g, st, yearMin, yearMax));
            merged.addAll(searchResultMapper.searchAnimationGlobal(sTrim, g, st, yearMin, yearMax));
            merged.addAll(searchResultMapper.searchNovelGlobal(sTrim, g, st, yearMin, yearMax));
        } else {
            switch (mtRaw) {
                case "comic":
                case "cartoon":
                case "manga":
                    merged.addAll(searchResultMapper.searchComicsGlobal(sTrim, g, st, yearMin, yearMax));
                    break;
                case "animation":
                case "donghua":
                    merged.addAll(searchResultMapper.searchAnimationGlobal(sTrim, g, st, yearMin, yearMax));
                    break;
                case "novel":
                    merged.addAll(searchResultMapper.searchNovelGlobal(sTrim, g, st, yearMin, yearMax));
                    break;
                default:
                    merged.addAll(searchResultMapper.searchComicsGlobal(sTrim, g, st, yearMin, yearMax));
                    merged.addAll(searchResultMapper.searchAnimationGlobal(sTrim, g, st, yearMin, yearMax));
                    merged.addAll(searchResultMapper.searchNovelGlobal(sTrim, g, st, yearMin, yearMax));
            }
        }

        merged.sort(buildComparator(normalizedSort));
        if (merged.size() > lim) {
            return new ArrayList<>(merged.subList(0, lim));
        }
        return merged;
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String normalizeSort(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "default";
        }
        String s = sortBy.trim().toLowerCase();
        if ("hot".equals(s) || "update".equals(s)) {
            return s;
        }
        return "default";
    }

    private static Comparator<SearchResult> buildComparator(String sortBy) {
        Comparator<SearchResult> byTypeThenId =
                Comparator.comparing(SearchResult::getSourcetype, Comparator.nullsLast(String::compareTo))
                        .thenComparingInt(SearchResult::getId);
        if ("hot".equals(sortBy)) {
            return Comparator.<SearchResult, BigDecimal>comparing(
                            r -> r.getRating() != null ? r.getRating() : BigDecimal.ZERO)
                    .reversed()
                    .thenComparing(SearchResult::getSourcetype, Comparator.nullsLast(String::compareTo))
                    .thenComparingInt(SearchResult::getId);
        }
        if ("update".equals(sortBy)) {
            return Comparator.<SearchResult, LocalDateTime>comparing(
                            r -> r.getUpdatedAt() != null ? r.getUpdatedAt() : LocalDateTime.MIN)
                    .reversed()
                    .thenComparing(SearchResult::getSourcetype, Comparator.nullsLast(String::compareTo))
                    .thenComparingInt(SearchResult::getId);
        }
        return byTypeThenId;
    }
}
