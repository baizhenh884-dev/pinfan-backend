-- KEYS[1] = seckill:stock:{voucherId}    库存 key
-- KEYS[2] = seckill:order:{voucherId}    订单用户集合 key
-- ARGV[1] = userId                        当前用户ID

-- 1. 检查库存
local stock = tonumber(redis.call('get', KEYS[1]))
if not stock or stock <= 0 then
    return 1   -- 库存不足
end

-- 2. 检查一人一单
if redis.call('sismember', KEYS[2], ARGV[1]) == 1 then
    return 2   -- 已经抢过
end

-- 3. 扣库存
redis.call('decr', KEYS[1])

-- 4. 记录订单标记
redis.call('sadd', KEYS[2], ARGV[1])

return 0   -- 成功