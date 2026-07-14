package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // 对当前路径下的所有请求都应用当前的跨域配置
                .allowedOrigins("*")  // 允许的跨域源
                .allowedMethods("*")  // 允许的请求方法
                .allowedHeaders("*")  // 允许的请求头
                .allowCredentials(true)  // 是否允许发送 Cookie
                .maxAge(3600);  // 设置请求最大有效时长
    }
}