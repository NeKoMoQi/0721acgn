package com.example.demo.service;

import com.example.demo.com.Galgame;

import java.util.List;

public interface GalgameService {
    List<Galgame> getGalgamePage(int offset, String genre, Integer yearMin, Integer yearMax, String sortBy);

    List<Galgame> getGalgameHot();

    Galgame getGalgameById(int id);
}
