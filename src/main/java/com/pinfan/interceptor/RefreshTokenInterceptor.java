package com.pinfan.interceptor;

import cn.hutool.core.util.StrUtil;
import com.pinfan.entity.User;
import com.pinfan.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RefreshTokenInterceptor implements HandlerInterceptor {

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从请求头读 token
        String token = request.getHeader("Authorization");
        if (StrUtil.isBlank(token)) {
            return true;   // 没 token，直接放行（让 LoginInterceptor 决定）
        }

        // 用 token 查 Redis 拿 user
        String key = "cache:token:" + token;
        Object userObj = redisTemplate.opsForValue().get(key);
        if (userObj == null) {
            return true;   // token 失效（过期/伪造），放行让 LoginInterceptor 决定
        }

        // user 存进 ThreadLocal
        User user = (User) userObj;
        UserHolder.saveUser(user);

        // 刷新 token 的 TTL（用户活跃就续命，30分钟无操作才过期）
        redisTemplate.expire(key, 30, TimeUnit.MINUTES);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserHolder.removeUser();
    }
}
