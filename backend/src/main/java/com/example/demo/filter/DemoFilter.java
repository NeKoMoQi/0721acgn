package com.example.demo.filter;

import com.example.demo.utils.JwtUtils;
import com.example.demo.utils.PublicAccessPolicy;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
// asyncSupported 必须开：圣地巡礼 Agent 的 SSE 接口依赖 Servlet 异步，否则启动异步时会 500。
@WebFilter(urlPatterns = "/*", asyncSupported = true)
public class DemoFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(DemoFilter.class);

    @Override //初始化
    public void init(FilterConfig filterConfig) throws ServletException {
       System.out.println("DemoFilter init");
    }
    @Override //请求使用
    public void doFilter(ServletRequest Request, ServletResponse Response, FilterChain filterChain) throws IOException, ServletException {
      System.out.println("DemoFilter doFilter");
      HttpServletRequest request = (HttpServletRequest) Request;
      HttpServletResponse response = (HttpServletResponse) Response;
        String origin = request.getHeader("Origin");
        if (origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
        }
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader(
                "Access-Control-Allow-Headers",
                "Content-Type, Authorization, Token, token, X-Acgn-Client");
        response.setHeader("Access-Control-Allow-Credentials", "true");

        // 处理 OPTIONS 请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (PublicAccessPolicy.isPublic(request.getMethod(), path)) {
            filterChain.doFilter(request, response);
            return;
        }
        //获取令牌
        String Jwt =  request.getHeader("Token");
        //判断是否存在令牌
        if(!StringUtils.hasLength(Jwt)){
            log.info("请求头token为空，返回未登录信息");
            PublicAccessPolicy.rejectUnauthorized(response);
            return;
        }
        //校验token
       try{
           JwtUtils.parseJwt(Jwt);
       }catch (Exception e){
           e.printStackTrace();
           log.info("解析令牌失败，返回未登录错误信息");
           PublicAccessPolicy.rejectUnauthorized(response);
           return;
       }

      //放行
        log.info("令牌合法，放行");
        filterChain.doFilter(Request, Response);
    }
    @Override //销毁
    public void destroy() {
        System.out.println("DemoFilter destroy");
    }
}
