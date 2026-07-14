package com.example.demo.mapper.mysql;

import com.example.demo.com.Activity;
import com.example.demo.com.LeiBie;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ActivityMapper {
    List<Activity> getActivityById(int a);
    List<Activity> getallActivity();
    List<Activity> gethotActivity();
    List<LeiBie> getLiebieActivity();
}
