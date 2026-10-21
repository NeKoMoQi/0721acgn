package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 跨域响应头统一由 DemoFilter 输出。
        // 这里原本是 allowedOrigins("*") + allowCredentials(true)，
        // Spring 6 下该组合会直接抛 IllegalArgumentException（登录 500），故留空。
    }
}