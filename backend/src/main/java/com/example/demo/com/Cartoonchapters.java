package com.example.demo.com;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Cartoonchapters {
    private int id;
    private String chapterid;
    private int comicid;
    private int chapterno;
    private String title;
}
