package com.example.demo.service.impl;

import com.example.demo.com.User;
import com.example.demo.mapper.mysql.UserMapper;
import com.example.demo.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    private UserMapper userMapper;
    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }
    @Override
    public User login (User user) {
        return userMapper.gerByUsernameAndPassword(user);
    }
    @Override
    public int changetime(User user){
        return userMapper.changetime(user);
    }
    public User finduser(String username,String password){
        return userMapper.finduser(username,password);
    }
}
