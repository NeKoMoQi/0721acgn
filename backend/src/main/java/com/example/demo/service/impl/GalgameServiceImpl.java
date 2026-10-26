package com.example.demo.service.impl;

import com.example.demo.com.Galgame;
import com.example.demo.mapper.mysql.GalgameMapper;
import com.example.demo.service.GalgameService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GalgameServiceImpl implements GalgameService {
    private final GalgameMapper galgameMapper;

    public GalgameServiceImpl(GalgameMapper galgameMapper) {
        this.galgameMapper = galgameMapper;
    }

    @Override
    public List<Galgame> getGalgamePage(int offset, String genre, Integer yearMin, Integer yearMax, String sortBy) {
        return galgameMapper.getGalgame(offset, genre, yearMin, yearMax, sortBy);
    }

    @Override
    public List<Galgame> getGalgameHot() {
        return galgameMapper.getGalgameHot();
    }

    @Override
    public Galgame getGalgameById(int id) {
        return galgameMapper.getGalgameById(id);
    }
}
