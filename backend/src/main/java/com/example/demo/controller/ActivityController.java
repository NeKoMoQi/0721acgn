package com.example.demo.controller;

import com.example.demo.com.Activity;
import com.example.demo.com.LeiBie;
import com.example.demo.com.Result;
import com.example.demo.service.ActivityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
public class ActivityController {
    @Autowired
    private ActivityService activityService;
    @GetMapping("/api/activity")
    public Result getActivity(@RequestParam int page){
        Integer a = (page-1)*9;
        List<Activity> activity = activityService.getActivity(a);
        return Result.success(activity);
    }
    @GetMapping("/api/activity/all")
    public Result getAllActivity(){
        List<Activity> activities = activityService.getallActivity();
        return Result.success(activities);
    }
    @GetMapping("/api/activity/hot")
    public Result getHotActivity(){
        List<Activity> activitiehot = activityService.gethotActivity();
        return Result.success(activitiehot);
    }
    @GetMapping("/api/activity/leibie")
    public Result getLiebieActivity(){
        List<LeiBie> activitiesleibie = activityService.getLiebieActivity();
        return Result.success(activitiesleibie);
    }
}
