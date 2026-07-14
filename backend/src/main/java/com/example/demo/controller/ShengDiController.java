package com.example.demo.controller;

import com.example.demo.com.Result;
import com.example.demo.com.Shengdi;
import com.example.demo.service.ShengDiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class ShengDiController {
    @Autowired
    private ShengDiService shengDiService;
    @GetMapping("/api/shengdi/hot")
    public Result getHotShengDi() {
        List<Shengdi> hot = shengDiService.selectALLhot();
        return Result.success(hot);
    }
}
