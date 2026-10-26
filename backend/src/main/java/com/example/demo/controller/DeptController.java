package com.example.demo.controller;

import com.example.demo.com.Result;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
@Slf4j
@RestController
public class DeptController {
    @GetMapping("/depts")
    public Result list() {
        log.info("查询全部部门数据");
        return Result.success();
    }
}
