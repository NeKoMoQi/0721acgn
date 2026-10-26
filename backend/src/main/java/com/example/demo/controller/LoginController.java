package com.example.demo.controller;
import com.example.demo.com.User;

import com.example.demo.service.UserService;
import com.example.demo.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import  org.springframework.web.bind.annotation.*;

import com.example.demo.com.Result;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
public class LoginController {
    @Autowired
    private UserService userService;
    private JwtUtils jwtUtils;
    @PostMapping("/api/admin/acl/index/login")
    public Result Login(@RequestBody User user) {
        log.info("用户登录:{}",user);
        User n = userService.login(user);
        int L = userService.changetime(user);
        System.out.println(L);
        //登陆成功，下发令牌
        if (n != null) {
            log.info("用户登录成功: {}", n.getUsername());
            Map<String,Object> claims = new HashMap<>();
            claims.put("id",n.getId());
            claims.put("username",n.getUsername());
            claims.put("password",n.getPassword());
            String jwt = JwtUtils.generateJwt(claims);//包含当前员工信息
            return Result.success(jwt);
        }
        return Result.error("用户名或密码错误");
    }
    @GetMapping("/api/admin/acl/index/info")
    public Result getuserinfo (@RequestHeader("token") String token) {
        log.info("用户登录");
        System.out.println(token);
        Claims up = jwtUtils.parseJwt(token);
        System.out.println(up);
         Object nousername = up.get("username");
         Object nopassword = up.get("password");
         String a = String.valueOf(nousername);
         String b = String.valueOf(nopassword);
         System.out.println(a);
         User user = userService.finduser(a,b);
         System.out.println(user);
        return Result.success(user);
    }
    @PostMapping("/api/admin/acl/index/logout")
    public Result logout(@RequestHeader("token") String token) {
        log.info("用户登出");
        return Result.success();
    }
}
