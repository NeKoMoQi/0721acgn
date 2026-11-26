package com.example.demo.dto;

public record SearchCriteria(String search, String genre, String status, Integer yearMin, Integer yearMax,
                             String sortBy, String mediaType, int limit, long offset) {}
