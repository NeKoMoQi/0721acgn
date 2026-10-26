package com.example.demo.service;

import com.example.demo.com.User;

import java.time.LocalDateTime;

public interface UserService {
    User login (User user);
    int changetime (User user);
    User finduser (String username,String password);
}
