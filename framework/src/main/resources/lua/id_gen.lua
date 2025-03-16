local _id_gen = function(incrKey, bitmapKey)
    local offset
    repeat
        -- 增加计数器
        offset = redis.call('INCR', incrKey)
        -- 检查位图中的位是否已经被设置
    until redis.call('GETBIT', bitmapKey, offset) == 0

    -- 设置位图中的位
    redis.call('SETBIT', bitmapKey, offset, 1)

    -- 返回最终的offset
    return offset
end

-- KEYS[1] 是计数器键
-- KEYS[2] 是位图键
return _id_gen(KEYS[1], KEYS[2])
