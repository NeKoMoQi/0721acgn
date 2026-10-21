package com.example.demo.config;

import com.example.demo.Interceptor.LoginCheakInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private LoginCheakInterceptor loginCheakInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // CORS headers are emitted by DemoFilter for every response; keep Spring's own
        // CORS processing out of the way so it can never reject a valid request with
        // "Invalid CORS request" (403) when the site is served from a public IP/domain.
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginCheakInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/activity", "/peoplecount", "/acgn/**", "/api/animation/**");
    }
}
