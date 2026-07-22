-- Redis Lua 秒杀脚本
-- 原子执行：库存检查 → 防重复 → 扣减 → 记录用户
--
-- KEYS[1] = templateId（盲盒模板 ID）
-- ARGV[1] = userId（用户 ID）
--
-- 返回值：
--   1  = 秒杀成功（已扣减库存）
--   0  = 库存不足
--   -1 = 重复购买（同一用户已抢过该盲盒）

local stockKey = 'seckill:stock:' .. KEYS[1]
local userSetKey = 'seckill:users:' .. KEYS[1]

-- 1. 检查库存（GET 返回 nil 时视为 0）
local stock = tonumber(redis.call('GET', stockKey) or '0')
if stock <= 0 then
    return 0
end

-- 2. 防重复购买（同一用户同一盲盒只能抢一次）
if redis.call('SISMEMBER', userSetKey, ARGV[1]) == 1 then
    return -1
end

-- 3. 原子扣减库存 + 记录用户
redis.call('DECR', stockKey)
redis.call('SADD', userSetKey, ARGV[1])

-- 4. 设置用户集合过期时间（24h），避免长期占用 Redis 内存
redis.call('EXPIRE', userSetKey, 86400)

return 1
