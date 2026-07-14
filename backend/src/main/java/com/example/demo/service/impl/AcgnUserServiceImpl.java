package com.example.demo.service.impl;

import com.example.demo.com.AcgnUser;
import com.example.demo.mapper.mysql.AcgnUserMapper;
import com.example.demo.service.AcgnUserService;
import org.springframework.stereotype.Service;

@Service
public class AcgnUserServiceImpl implements AcgnUserService {
    private AcgnUserMapper acgnUserMapper;
    public AcgnUserServiceImpl(AcgnUserMapper acgnUserMapper) {
        this.acgnUserMapper = acgnUserMapper;
    }
    public AcgnUser selectByRandom(int id){
        return acgnUserMapper.selectByRandom(id);
    }
    @Override
    public AcgnUser selectOneRandom() {
        return acgnUserMapper.selectOneRandom();
    }
    public  int shujucount(){
        return acgnUserMapper.shujucount();
    }

    @Override
    public AcgnUser selectBybirth(int month, int day) {
        return acgnUserMapper.selectBybirth(month, day);
    }
}
