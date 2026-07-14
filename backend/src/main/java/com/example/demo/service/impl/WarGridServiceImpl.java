package com.example.demo.service.impl;

import com.example.demo.com.WarGrid;
import com.example.demo.mapper.postgresql.WarGridMapper;
import com.example.demo.service.WarGridService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WarGridServiceImpl implements WarGridService {

    private final WarGridMapper wargridmapper;

    // 这是构造函数，不是 void！
    public WarGridServiceImpl(WarGridMapper wargridmapper) {
        this.wargridmapper = wargridmapper;
    }

    @Override
    public List<WarGrid> getWarGrid() {
        return wargridmapper.getWarGrid();
    }
}
