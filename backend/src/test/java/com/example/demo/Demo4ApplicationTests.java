package com.example.demo;

import com.example.demo.com.Emp;
import com.example.demo.mapper.mysql.EmpMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

//@SpringBootTest
class Demo4ApplicationTests {

    @Test
    void contextLoads() {
    }
    @Autowired
    private EmpMapper empMapper;
    @Test
    public void test() {
        List<Emp> empList = empMapper.showdb();
        empList.forEach(System.out::println); // 使用 Lombok 的 @Data 确保 toString 可用
        System.out.println("Emp list size: " + empList.size());
    }
    @Test
    public void test1() {
        int delete = empMapper.delete(20);
        System.out.println(delete);
    }
    @Test
    public void test2() {
        Emp emp = new Emp();
        emp.setId(7);
        emp.setUsername("moqi11");
        emp.setPassword("123456");
        emp.setName("万俟11");
        emp.setGender("男");
        emp.setJob(2);
        emp.setEntryday(LocalDate.of(2000,1,1));
        emp.setCreatetime(LocalDateTime.now());
        emp.setUpdatetime(LocalDateTime.now());
        emp.setDpt_id(2);
        empMapper.add(emp);
        System.out.println(emp);
        System.out.println(emp.getId());
    }
    @Test
    public void test3() {
        Emp emp = new Emp();
        emp.setId(7);
        emp.setUsername("moq2221112");
        emp.setName("万俟11122");
        emp.setGender("男");
        emp.setUpdatetime(LocalDateTime.now());
        empMapper.update(emp);
        System.out.println(emp);
        System.out.println(emp.getId());
    }
    @Test
    public void test4() {
        Emp empList = empMapper.findById(3);
        System.out.println(empList);
    }
    @Test
    public void test5() {
        List<Emp> empList = empMapper.findByName(null, "男", null, null);
        System.out.println(empList);
    }
    @Test
    public void test6() {
        List<Integer> ids = Arrays.asList(17,18,19);
        empMapper.Deletes(ids);
    }
    @Test
    public void test7() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", "moqi11");
        claims.put("password", "123456");
        String jwt = Jwts.builder()
                .signWith(SignatureAlgorithm.HS256,"0721ACGN")//签名算法
                .setClaims(claims)//载荷
                .setExpiration(new Date(System.currentTimeMillis() + 3600*1000))//设置有效期
                .compact();
        System.out.println(jwt);
    }
    @Test
//    解析令牌
    public void test8() {
       Claims claims = Jwts.parser()
                .setSigningKey("0721ACGN")
                .parseClaimsJws("eyJhbGciOiJIUzI1NiJ9.eyJwYXNzd29yZCI6IjEyMzQ1NiIsImV4cCI6MTc0MTAyODA4NCwidXNlcm5hbWUiOiJtb3FpMTEifQ.TMYKHZQgOmMBgbdW29hoS3bZ4i8qtI5iqpko8z2lLps")
                .getBody();
        System.out.println(claims);
    }
}
