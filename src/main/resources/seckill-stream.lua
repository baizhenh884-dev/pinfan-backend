-- KEYS[1] = seckill:stock:{voucherId}
-- KEYS[2] = seckill:order:{voucherId}
-- KEYS[3] = stream.orders (Stream key)
-- ARGV[1] = voucherId
-- ARGV[2] = userId
-- ARGV[3] = orderId

-- 1. 检查库存
local stock = tonumber(redis.call('get', KEYS[1]))
if not stock or stock <= 0 then
    return 1
end

-- 2. 检查一人一单
if redis.call('sismember', KEYS[2], ARGV[2]) == 1 then
    return 2
end

-- 3. 扣库存
redis.call('decr', KEYS[1])

-- 4. 记录订单标记
redis.call('sadd', KEYS[2], ARGV[2])

-- 5. 投递消息到 Stream（关键新增）
redis.call('xadd', KEYS[3], '*',
    'voucherId', ARGV[1],
    'userId', ARGV[2],
    'id', ARGV[3])

return 0