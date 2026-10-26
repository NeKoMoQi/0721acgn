package com.example.demo.controller;

import com.example.demo.com.RelatedWorkVo;
import com.example.demo.com.Result;
import com.example.demo.service.RelatedWorkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class RelatedWorkController {

    @Autowired
    private RelatedWorkService relatedWorkService;

    @GetMapping("/api/related")
    public Result listRelated(
            @RequestParam String srcType,
            @RequestParam int srcId) {
        List<RelatedWorkVo> list = relatedWorkService.listBySource(srcType.trim(), srcId);
        return Result.success(list);
    }
}
