package com.example.demo.mapper.mysql;
import com.example.demo.com.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {
    User gerByUsernameAndPassword(User user);
    List<User> selectloaduser();
    int changetime (User user);
    User finduser(String username, String password);
}
