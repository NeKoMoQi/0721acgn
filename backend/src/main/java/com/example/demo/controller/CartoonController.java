package com.example.demo.controller;

import com.example.demo.com.Animation;
import com.example.demo.com.CartoonPage;
import com.example.demo.com.Cartoons;
import com.example.demo.com.Result;
import com.example.demo.service.CartoonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class CartoonController {
    @Autowired
    private CartoonService cartoonService;

    @GetMapping("/api/cartoon")
    public Result listCartoons(
            @RequestParam int page,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer yearMin,
            @RequestParam(required = false) Integer yearMax,
            @RequestParam(required = false, defaultValue = "default") String sortBy) {
        int offset = (page - 1) * 20;
        List<Cartoons> cartoonslist = cartoonService.getCartoonsPage(
                offset,
                emptyToNull(genre),
                emptyToNull(status),
                yearMin,
                yearMax,
                normalizeSort(sortBy));
        return Result.success(cartoonslist);
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String normalizeSort(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "default";
        }
        String s = sortBy.trim().toLowerCase();
        return ("hot".equals(s) || "update".equals(s)) ? s : "default";
    }
    @GetMapping("/api/cartoon/byid")
    public Result getCartoonById(@RequestParam int id) {
        Cartoons cartoonslist = cartoonService.getCartoonsbyid(id);
        return Result.success(cartoonslist);
    }
    @GetMapping("/api/cartoonpage")
    public Result getCartoonPage(@RequestParam String chapter,int page) {
        Integer start = (page-1)*4;
        List<CartoonPage> cartoonPagesList = cartoonService.getCartoonpages(chapter,start);
        return Result.success(cartoonPagesList);
    }
    @GetMapping("/api/cartoon/hot")
    public Result getHotcartoon() {
        List<Cartoons> b = cartoonService.getCartoonsHot();
        return Result.success(b);
    }
}
