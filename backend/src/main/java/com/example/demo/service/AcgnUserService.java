package com.example.demo.service;

import com.example.demo.com.AcgnUser;

public interface AcgnUserService {
    AcgnUser selectByRandom(int id);
    AcgnUser selectOneRandom();
    int shujucount();
    AcgnUser selectBybirth(int month, int day);
}
