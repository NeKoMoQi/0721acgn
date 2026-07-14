package com.example.demo.mapper.mysql;

import com.example.demo.com.AcgnUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AcgnUserMapper {
    AcgnUser selectByRandom(int id);
    AcgnUser selectOneRandom();
    int shujucount();
    AcgnUser selectBybirth(@Param("month") int month, @Param("day") int day);
}
