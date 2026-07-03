package com.pinfan.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class RedisIdWorker {

    /** 起始时间戳（2024-01-01 00:00:00 UTC 的秒数），高位相对这个算 */
    private static final long BEGIN_TIMESTAMP = 1704067200L;

    /** 低位占 32 位 */
    private static final int COUNT_BITS = 32;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 生成全局唯一 ID
     * @param keyPrefix 业务前缀，用来分不同的计数器空间（"order" / "voucher" 等）
     */
    public long nextId(String keyPrefix) {
        // 1. 高位：当前秒 - 起始秒
        long nowSecond = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;

        // 2. 低位：Redis 按日累加序列
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);

        // 3. 拼接（高 32 位 + 低 32 位）
        return timestamp << COUNT_BITS | count;
    }
}