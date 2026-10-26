package com.example.demo.service.impl;

import com.example.demo.com.Cartoonchapters;
import com.example.demo.mapper.mysql.CartoonChaptersMapper;
import com.example.demo.service.CartoonChaptersService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartoonChaptersServiceImpl implements CartoonChaptersService {
    private CartoonChaptersMapper cartoonChaptersMapper;
    public CartoonChaptersServiceImpl(CartoonChaptersMapper cartoonChaptersMapper){
        this.cartoonChaptersMapper = cartoonChaptersMapper;
    }
    public List<Cartoonchapters> getAllCartoonChapters(int id){
        return cartoonChaptersMapper.getAllCartoonChapters(id);
    };
}
