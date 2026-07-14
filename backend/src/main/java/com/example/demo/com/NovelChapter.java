package com.example.demo.com;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class NovelChapter {
    private int id;
    private String chapterid;
    private int novelid;
    private int chapterno;
    private String title;
    private String novelurl;
}
