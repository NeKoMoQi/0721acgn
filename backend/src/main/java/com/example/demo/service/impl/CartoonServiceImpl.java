package com.example.demo.service.impl;

import com.example.demo.com.CartoonPage;
import com.example.demo.com.Cartoons;
import com.example.demo.mapper.mysql.CartoonMapper;
import com.example.demo.service.CartoonService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartoonServiceImpl implements CartoonService {
    private CartoonMapper cartoonMapper;
    public CartoonServiceImpl(CartoonMapper cartoonMapper) {
        this.cartoonMapper = cartoonMapper;
    }
    public List<Cartoons> getCartoonsPage(
            int offset,
            String genre,
            String status,
            Integer yearMin,
            Integer yearMax,
            String sortBy) {
        return cartoonMapper.getCartoons(offset, genre, status, yearMin, yearMax, sortBy);
    }
    public Cartoons getCartoonsbyid(int id){
        return cartoonMapper.getCartoonsbyid(id);
    }
    public List<CartoonPage> getCartoonpages(String chapter,int page){
        return cartoonMapper.getCartoonpages(chapter,page);
    }
    public List<Cartoons> getCartoonsHot (){
        return cartoonMapper.getCartoonsHot();
    }
}
