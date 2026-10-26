package com.example.demo.com;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Activity {
    private int id;
    private String activityname;
    private String activityimg;   // 原 activity_img
    private String activitydescribe; // 原 activity_describe
    private LocalDateTime createtime;
    private String createname;
    private int click;
}
