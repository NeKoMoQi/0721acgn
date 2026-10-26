package com.example.demo.mapper.mysql;

import com.example.demo.com.People;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PeopleMapper {
    People selectPeopleCount();
}
