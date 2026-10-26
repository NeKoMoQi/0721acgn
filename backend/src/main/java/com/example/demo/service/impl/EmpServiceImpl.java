package com.example.demo.service.impl;

import com.example.demo.com.Emp;
import com.example.demo.mapper.mysql.EmpMapper;
import com.example.demo.service.EmpService;
import org.springframework.stereotype.Service;

@Service
public class EmpServiceImpl implements EmpService {
    private  EmpMapper empMapper;
    public EmpServiceImpl(EmpMapper empMapper) {
        this.empMapper = empMapper;
    }
    @Override
    public Emp login (Emp emp) {
        return empMapper.gerByUsernameAndPassword(emp);
    }
}
