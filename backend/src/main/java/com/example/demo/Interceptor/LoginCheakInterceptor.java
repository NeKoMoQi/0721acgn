package com.example.demo.Interceptor;

import com.alibaba.fastjson.JSONObject;
import com.example.demo.com.Result;
import com.example.demo.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
@Slf4j
@Component
public class LoginCheakInterceptor implements HandlerInterceptor {
    @Override //目标资源方法运行前运行，返回true，放行，返回false，不放行
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取请求url
        String url = request.getRequestURI().toString();
        log.info(request.getRequestURI());
        //判断是否为登录操作
        if(url.contains("login")||url.contains("/activity")||url.contains("/peoplecount")||url.contains("/acgn")||url.contains("/shengdi/hot")){
            log.info("登录请求，放行");
            return true;
        }
        //获取令牌
        String Jwt =  request.getHeader("token");
        //判断是否存在令牌
        if(!StringUtils.hasLength(Jwt)){
            log.info("请求头token为空，返回未登录信息");
            Result error = Result.error("NOT_LOGIN");
            //手动转换
            String notLogin = JSONObject.toJSONString(error);
            response.getWriter().write(notLogin);
            return false;
        }
        //校验token
        try{
            JwtUtils.parseJwt(Jwt);
        }catch (Exception e){
            e.printStackTrace();
            log.info("解析令牌失败，返回未登录错误信息");
            Result error = Result.error("NOT_LOGIN");
            //手动转换
            String notLogin = JSONObject.toJSONString(error);
            response.getWriter().write(notLogin);
            return false;
        }

        //放行
        log.info("令牌合法，放行");
        return true;
    }
    @Override //目标资源方法运行后运行
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        System.out.println("postHandle");

    }
    @Override //视图渲染完毕后运行，最后运行
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        System.out.println("afterCompletion");
    }
}
