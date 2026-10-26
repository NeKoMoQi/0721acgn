package com.example.demo.service.impl;

import com.example.demo.com.People;
import com.example.demo.mapper.mysql.PeopleMapper;
import com.example.demo.service.PeopleService;
import org.springframework.stereotype.Service;

@Service
public class PeopleServiceImpl implements PeopleService {
    private PeopleMapper peopleMapper;
    public PeopleServiceImpl(PeopleMapper peopleMapper) {
        this.peopleMapper = peopleMapper;
    }
    public People selectPeopleCount() {
        return peopleMapper.selectPeopleCount();
    }
}
