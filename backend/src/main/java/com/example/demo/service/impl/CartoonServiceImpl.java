package com.example.demo.service.impl;

import com.example.demo.com.CartoonPage;
import com.example.demo.com.Cartoons;
import com.example.demo.mapper.mysql.CartoonMapper;
import com.example.demo.service.CartoonService;
import com.example.demo.util.ImageCdnRewriter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartoonServiceImpl implements CartoonService {
    private CartoonMapper cartoonMapper;
    public CartoonServiceImpl(CartoonMapper cartoonMapper) {
        this.cartoonMapper = cartoonMapper;
    }

    /** 库里存的是 OSS 原始地址，出接口前统一换成图片 CDN（数据库不改）。 */
    public List<Cartoons> getCartoonsPage(
            int offset,
            String genre,
            String status,
            Integer yearMin,
            Integer yearMax,
            String sortBy) {
        return ImageCdnRewriter.applyCdnToCartoons(
                cartoonMapper.getCartoons(offset, genre, status, yearMin, yearMax, sortBy));
    }

    public Cartoons getCartoonsbyid(int id){
        return ImageCdnRewriter.applyCdn(cartoonMapper.getCartoonsbyid(id));
    }

    /** 阅读页图片：fileurl 里的 OSS 直链换成 CDN，缩图参数 x-oss-process 原样保留。 */
    public List<CartoonPage> getCartoonpages(String chapter,int page){
        return ImageCdnRewriter.applyCdnToCartoonPages(cartoonMapper.getCartoonpages(chapter,page));
    }

    public List<Cartoons> getCartoonsHot (){
        return ImageCdnRewriter.applyCdnToCartoons(cartoonMapper.getCartoonsHot());
    }
}
