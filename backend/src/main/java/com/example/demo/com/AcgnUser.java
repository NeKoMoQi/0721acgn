package com.example.demo.com;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AcgnUser {
    private int id;
    private String name;
    private String sex;
    private String avatarurl;
    private String birthday;
    private String talent;
}
