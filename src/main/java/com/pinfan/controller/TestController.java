package com.pinfan.controller;

import com.pinfan.common.exception.BusinessException;
import com.pinfan.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 测试接口：验证项目骨架联通性
 */
@Slf4j
@RestController
@RequestMapping("/test")
@Tag(name = "00-测试接口", description = "服务存活和中间件连通性检查")
public class TestController {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/ping")
    @Operation(summary = "服务存活检查")
    public String ping() {
        log.info("收到 ping 请求");
        return "pong from pinfan-backend @ " + LocalDateTime.now();
    }

    @GetMapping("/redis")
    @Operation(summary = "Redis 读写连通性检查")
    public String testRedis() {
        String key = "pinfan:test:" + System.currentTimeMillis();
        stringRedisTemplate.opsForValue().set(key, "hello redis", 60, TimeUnit.SECONDS);
        String val = stringRedisTemplate.opsForValue().get(key);
        return "Redis OK, key=" + key + ", value=" + val;
    }

    @GetMapping("/redis/obj")
    @Operation(summary = "测试 Redis 存对象（JSON 序列化）")
    public R<Object> testRedisObject() {
        // 故意用 HashMap 而不是 Map.of()
        // Map.of() 返回的 ImmutableCollections$MapN 是 JDK 内部不可变实现，构造函数私有，Jackson 反序列化时会挂
        Map<String, Object> obj = new HashMap<>();
        obj.put("id", 1);
        obj.put("name", "海底捞");
        obj.put("score", 4.8);
        redisTemplate.opsForValue().set("pinfan:test:shop", obj, 60, TimeUnit.SECONDS);
        Object back = redisTemplate.opsForValue().get("pinfan:test:shop");
        return R.ok("OK", back);
    }

    @GetMapping("/error/business")
    @Operation(summary = "故意抛业务异常 → 验证 GlobalExceptionHandler 是否拦截")
    public R<String> testBusinessError() {
        throw new BusinessException(409, "用户已存在");
    }

    @GetMapping("/error/system")
    @Operation(summary = "故意抛系统异常 → 验证兜底处理")
    public R<String> testSystemError() {
        String s = null;
        return R.ok(String.valueOf(s.length())); // 必然空指针
    }
}
