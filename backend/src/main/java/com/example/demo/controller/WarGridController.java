package com.example.demo.controller;

import com.example.demo.com.Result;
import com.example.demo.com.WarGrid;
import com.example.demo.com.Xunli;
import com.example.demo.service.WarGridService;
import com.example.demo.service.XunliService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@Slf4j
@RestController
public class WarGridController {
    @Autowired
    private WarGridService wargridService;
    @GetMapping("/api/warflag/getgrid")
    public Result getWarGrid (){
        List<WarGrid> warGrids = wargridService.getWarGrid();
        return Result.success(warGrids);
    }
}
