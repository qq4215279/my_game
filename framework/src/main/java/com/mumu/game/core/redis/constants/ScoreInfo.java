package com.mumu.game.core.redis.constants;

import org.springframework.data.redis.core.ZSetOperations.TypedTuple;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ScoreInfo
 * 玩家redis积分信息
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:37
 */
@Data
@NoArgsConstructor
public class ScoreInfo {
    private long playerId;
    private long score;
    private int rank;

    public ScoreInfo(long playerId) {
        this.playerId = playerId;
    }

    public ScoreInfo(TypedTuple<String> tuple, int rank) {
        this.playerId = Long.parseLong(tuple.getValue());
        this.score = (long)Math.ceil(tuple.getScore());
        this.rank = rank;
    }
}
