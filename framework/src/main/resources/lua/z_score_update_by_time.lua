local _zscore_upt_time = function(key, member, addVal, timeStr, removeNegative)
    local add = tonumber(addVal)
    local time = tonumber(timeStr)
    local score = redis.call("zscore", key, member)
    if score == false then
        score = 0
    else
        score = math.ceil(score)
    end
    local yz = math.pow(10, 10)
    -- 变更后的积分不能为负数
    local newScore = add + score
    if newScore <= 0 and removeNegative == 'true' then
        redis.call('zrem', key, member)
        return 0
    end
    score = newScore - time / yz
    redis.call('zadd', key, score, member)
    return newScore
end
return _zscore_upt_time(KEYS[1], KEYS[2], KEYS[3], KEYS[4], KEYS[5])
