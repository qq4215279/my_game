local _zset_get_rank_with_score = function(zsetKey, elements)
    -- 创建一个表来存储结果
    local results = {}

    -- 遍历每个元素，获取它的排名和积分
    for i, element in ipairs(elements) do
        local rank = redis.call('ZREVRANK', zsetKey, element)
        if rank then
            local score = redis.call('ZSCORE', zsetKey, element)
            -- ZREVRANK 返回的是基于0的索引，所以加1以得到可读的排名
            table.insert(results, { playerId = element, rank = rank + 1, score = math.ceil(score) })
        else
            table.insert(results, { playerId = element, rank = 0, score = 0 })
        end
    end

    -- 返回结果
    return cjson.encode(results)
end

-- KEYS[1] 是有序集合的键
-- ARGV 包含要查询的元素列表
return _zset_get_rank_with_score(KEYS[1], ARGV)
