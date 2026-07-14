package com.example.demo.controller;

import com.example.demo.com.Cartoonchapters;
import com.example.demo.com.Result;
import com.example.demo.service.CartoonChaptersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class CartoonChaptersController {
    @Autowired
    private CartoonChaptersService cartoonChaptersService;
    @GetMapping("/api/cartoon/chapter")
    public Result getCartoonChapters(@RequestParam int id) {
        List<Cartoonchapters> cartoonchapters = cartoonChaptersService.getAllCartoonChapters(id);
        return Result.success(cartoonchapters);
    }
}
