package com.example.demo.service;

import com.example.demo.com.CartoonPage;
import com.example.demo.com.Cartoons;

import java.util.List;

public interface CartoonService {
    List<Cartoons> getCartoonsPage(
            int offset,
            String genre,
            String status,
            Integer yearMin,
            Integer yearMax,
            String sortBy);
    Cartoons getCartoonsbyid(int id);
    List<CartoonPage> getCartoonpages(String chapter,int page);
    List<Cartoons> getCartoonsHot ();
}
