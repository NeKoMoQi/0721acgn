package com.example.demo.service.impl;

import com.example.demo.com.Shengdi;
import com.example.demo.mapper.mysql.ShengDiMapper;
import com.example.demo.service.ShengDiService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShengDiServiceImpl implements ShengDiService {
    private ShengDiMapper shengDiMapper;
    public ShengDiServiceImpl(ShengDiMapper shengDiMapper) {
        this.shengDiMapper = shengDiMapper;
    }
    public List<Shengdi> selectALLhot(){
        return shengDiMapper.selectALLhot();
    }
}
