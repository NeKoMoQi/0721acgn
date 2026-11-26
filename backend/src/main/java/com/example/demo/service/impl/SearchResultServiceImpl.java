package com.example.demo.service.impl;

import com.example.demo.com.SearchResult;
import com.example.demo.dto.SearchCriteria;
import com.example.demo.mapper.mysql.SearchResultMapper;
import com.example.demo.service.SearchResultService;
import com.example.demo.util.ImageCdnRewriter;
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
        List<SearchResult> rows = new ArrayList<>(searchResultMapper.getSearchResult(search));
        if (search != null && !search.isBlank()) {
            String term = search.trim();
            rows.addAll(searchResultMapper.searchGalgame(term, null, null, null));
            rows.addAll(searchResultMapper.searchCharacters(term, 50));
            rows.sort(Comparator.comparingInt(row -> matchRank(row, term)));
        }
        return ImageCdnRewriter.applyCdnToSearchResults(rows);
    }

    @Override
    public Page searchPage(String search, String genre, String status, Integer yearMin, Integer yearMax,
                           String sortBy, String mediaType, int page, Integer limit) {
        int size = limit == null ? 24 : Math.min(60, Math.max(1, limit));
        int requested = Math.max(1, page);
        String term = search == null ? "" : search.trim();
        String type = mediaType == null ? "" : mediaType.trim().toLowerCase(java.util.Locale.ROOT);
        type = switch (type) {
            case "cartoon", "manga" -> "comic";
            case "donghua" -> "animation";
            case "comic", "animation", "novel", "galgame", "character" -> type;
            default -> "";
        };
        SearchCriteria criteria = new SearchCriteria(term, emptyToNull(genre), emptyToNull(status),
                yearMin, yearMax, normalizeSort(sortBy), type, size, 0);
        long total = searchResultMapper.countPage(criteria);
        int actual = (int) Math.min(requested, Math.max(1L, (total + size - 1) / size));
        SearchCriteria window = new SearchCriteria(term, criteria.genre(), criteria.status(), yearMin,
                yearMax, criteria.sortBy(), type, size, (long) (actual - 1) * size);
        return new Page(total == 0 ? List.of() : ImageCdnRewriter.applyCdnToSearchResults(searchResultMapper.searchPage(window)),
                total, actual, size);
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

        if ("character".equals(mtRaw)) {
            return ImageCdnRewriter.applyCdnToSearchResults(searchResultMapper.searchCharacters(sTrim, lim));
        }

        List<SearchResult> merged = new ArrayList<>();
        if (mtRaw.isEmpty()) {
            if (!sTrim.isEmpty() && g == null && st == null && yearMin == null && yearMax == null) {
                merged.addAll(searchResultMapper.searchCharacters(sTrim, Math.min(lim, 50)));
            }
            if (st == null) merged.addAll(searchResultMapper.searchGalgame(sTrim, g, yearMin, yearMax));
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
                case "galgame":
                    merged.addAll(searchResultMapper.searchGalgame(sTrim, g, yearMin, yearMax));
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

        Comparator<SearchResult> order = buildComparator(normalizedSort);
        if ("default".equals(normalizedSort) && !sTrim.isEmpty()) {
            order = Comparator.<SearchResult>comparingInt(row -> matchRank(row, sTrim)).thenComparing(order);
        }
        merged.sort(order);
        if (merged.size() > lim) {
            return ImageCdnRewriter.applyCdnToSearchResults(new ArrayList<>(merged.subList(0, lim)));
        }
        return ImageCdnRewriter.applyCdnToSearchResults(merged);
    }

    private static int matchRank(SearchResult row, String term) {
        if (row.getMatchRank() != null) return row.getMatchRank();
        String name = row.getName();
        if (name == null) return 2;
        if (name.equalsIgnoreCase(term)) return 0;
        return name.regionMatches(true, 0, term, 0, term.length()) ? 1 : 2;
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
