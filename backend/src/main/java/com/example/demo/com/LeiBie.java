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
public class LeiBie {
   private int id;
   private String Actleibie;
   private LocalDate createtime;
   private LocalDateTime updatetime;
   private int count;
}
