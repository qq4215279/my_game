package com.mumu.game.redis.constants;

import cn.hutool.core.util.StrUtil;

/**
 * RedisKey Redis key 枚举类 - 用于构建redis key
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:42
 */
public enum RedisKey {
    // ============================== 【玩家】==============================
    /** 玩家属性 */
    PLAYER_ATTRIBUTE("player:attribute:{}", RedisTypeEnum.HASH),

    /** 发布订阅渠道 */
    CHANNEL("channel:{}", RedisTypeEnum.STRING),

    /** 三方ID对应的验证码（%s 三方id，如手机号、邮箱， value-验证码） */
    ACCOUNT_THIRD_CODE("account:third:{}", RedisTypeEnum.STRING),
    /** token 信息（注意，此key游戏服与账号服共同使用，不要随意修改Key名称） */
    ACCOUNT_TOKEN("account:token:{}", RedisTypeEnum.STRING),
    /** 玩家id集合（注意，此key游戏服与账号服共同使用，不要随意修改Key名称） */
    PLAYER_ID_BITSET("player:id:bitset", RedisTypeEnum.STRING),
    /** 玩家id自增序列 */
    PLAYER_ID_GEN("player:id:gen", RedisTypeEnum.STRING),

    /** 玩家每日上传次数控制 %d-dayOfYear field-playerId */
    PLAYER_OSS_UPLOAD("oss:player_upload_count:{}", RedisTypeEnum.HASH),

    /** 谷歌支付accessToken */
    GOOGLE_CHARGE_ACCESS_TOKEN("charge:google_charge_access_token", RedisTypeEnum.STRING),
    /** 支付失败信息 */
    CHARGE_FAIL_RECORD("charge:fail:record:{}", RedisTypeEnum.STRING),

    /** 账号服接口限流key */
    ACCOUNT_RATE_LIMIT("account:rate_limit:{}", RedisTypeEnum.STRING),
  ;

    /** key 模板 */
    private final String template;

    /**
     * redisType 对应 redis 数据类型，仅用于声明
     */
    RedisKey(String template, RedisTypeEnum redisType) {
        this.template = template;
    }

    /**
     * 获取 redis key
     * @param params 构造key所需参数集
     * @return redis key
     */
    public String buildKey(Object... params) {
        return StrUtil.format(this.template, params);
    }

    /**
     * redis 数据类型
     */
    private enum RedisTypeEnum {
        STRING,
        HASH,
        LIST,
        SET,
        ZSET,
    }
}
