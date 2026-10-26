package com.example.demo.controller;

import com.example.demo.com.People;
import com.example.demo.com.Result;
import com.example.demo.service.PeopleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class PeopleController {
    @Autowired
    private PeopleService peopleService;
    @GetMapping("/api/peoplecount")
    public Result getPeopleCount() {
        People count = new People();
        count = peopleService.selectPeopleCount();
        return Result.success(count);
    }
}
