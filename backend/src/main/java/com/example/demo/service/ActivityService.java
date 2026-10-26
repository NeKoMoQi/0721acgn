package com.example.demo.service;

import com.example.demo.com.Activity;
import com.example.demo.com.LeiBie;

import java.util.List;

public interface ActivityService {
    List<Activity> getActivity(int a);
    Activity getActivityDetail(int id);
    List<Activity> getallActivity();
    List<Activity> gethotActivity();
    List<LeiBie> getLiebieActivity();
}

