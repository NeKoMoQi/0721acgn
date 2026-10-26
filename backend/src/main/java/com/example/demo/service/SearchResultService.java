package com.example.demo.service;

import com.example.demo.com.SearchResult;

import java.util.List;

public interface SearchResultService {
    List<SearchResult> getSearchResults(String search);

    List<SearchResult> getSearchResultsAdvanced(
            String search,
            String genre,
            String status,
            Integer yearMin,
            Integer yearMax,
            String sortBy,
            String mediaType,
            Integer limit
    );
}
