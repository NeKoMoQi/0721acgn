package com.example.demo.com;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Emp {
    private int id;
    private String username;
    private String password;
    private String name;
    private String gender;
    private int job;
    private LocalDate entryday;
    private LocalDateTime createtime;
    private LocalDateTime updatetime;
    private Integer dpt_id;
}
