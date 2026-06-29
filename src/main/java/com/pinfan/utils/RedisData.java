package com.pinfan.utils;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 逻辑过期缓存的包装类：把"业务数据"和"过期时间"放在一起存进 Redis
 */
@Data
public class RedisData {
    /** 逻辑过期时间 */
    private LocalDateTime expireTime;
    /** 实际业务数据 */
    private Object data;
}
