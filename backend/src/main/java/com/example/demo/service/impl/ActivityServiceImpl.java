package com.example.demo.service.impl;

import com.example.demo.com.Activity;
import com.example.demo.com.LeiBie;
import com.example.demo.mapper.mysql.ActivityMapper;
import com.example.demo.service.ActivityService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityServiceImpl implements ActivityService {
    private ActivityMapper activityMapper;
    public ActivityServiceImpl(ActivityMapper activityMapper) {
        this.activityMapper = activityMapper;
    }

    public List<Activity> getActivity(int a)
    {
        return activityMapper.getActivityById(a);
    }
    public Activity getActivityDetail(int id){
        return activityMapper.getActivityDetail(id);
    }
    public List<Activity> getallActivity(){
        return activityMapper.getallActivity();
    }
    public List<Activity> gethotActivity(){
        return activityMapper.gethotActivity();
    }
    public List<LeiBie> getLiebieActivity() {
        return activityMapper.getLiebieActivity();
    }
}

