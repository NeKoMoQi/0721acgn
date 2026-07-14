package com.example.demo.com;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CartoonPage {
    private int id;
    private String chapterid;
    private String pageid;
    private String fileurl;
    private int pageorder;
}

