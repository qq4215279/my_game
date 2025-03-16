local _zset_range_pop = function(zsetKey, minScoreStr, maxScoreStr, limitStr)
    local minScore = tonumber(minScoreStr)
    local maxScore = tonumber(maxScoreStr)
    local limit = tonumber(limitStr)

    local elements
    if limit > 0 then
        -- 获取指定数量的元素
        elements = redis.call('ZRANGEBYSCORE', zsetKey, minScore, maxScore, 'LIMIT', 0, limit)
    else
        -- 获取全部元素
        elements = redis.call('ZRANGEBYSCORE', zsetKey, minScore, maxScore)
    end

    -- 从zset中移除这些元素
    for _, element in ipairs(elements) do
        redis.call('ZREM', zsetKey, element)
    end
    -- 返回最终的offset
    return elements
end

-- KEYS[1] zsetKey
-- KEYS[2] minScore
-- KEYS[3] maxScore
-- KEYS[4] count（小于等0 查全部）
return _zset_range_pop(KEYS[1], KEYS[2], KEYS[3], KEYS[4])
