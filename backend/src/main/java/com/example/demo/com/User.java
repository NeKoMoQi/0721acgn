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
public class User {
    private int id;
    private String telephone;
    private String username;
    private String password;
    private String avatar;
    private LocalDate createtime;
    private LocalDateTime loadingtime;
    private LocalDateTime updatetime;
    private String level;
}
