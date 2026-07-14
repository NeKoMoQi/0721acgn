package com.example.demo.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.sql.Date;
import java.util.Map;

public class JwtUtils {
    private static String signKey = "0721ACGN";
    private static Long expire = 43200000L;
    public static String generateJwt(Map<String, Object> claims) {
        String jwt = Jwts.builder()
                .signWith(SignatureAlgorithm.HS256,signKey)//签名算法
                .setClaims(claims)//载荷
                .setExpiration(new Date(System.currentTimeMillis() + expire))//设置有效期
                .compact();
        return jwt;
    }
    public static Claims parseJwt(String jwt) {
        Claims claims = Jwts.parser()
                .setSigningKey("0721ACGN")
                .parseClaimsJws(jwt)
                .getBody();
        System.out.println(claims);
        return  claims;
    }
}
