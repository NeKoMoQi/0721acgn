package com.example.demo.com;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Novel {
    private int id;
    private int novelid;
    private String name;
    private String shortcomment;
    private String longcomment;
    private String author;
    private String picurl;
    private String hengpic;
    private int chaptercount;
    private String genre;
    private String status;
    private String releasedate;
    private BigDecimal rating;
    private LocalDateTime updatedAt;
}
