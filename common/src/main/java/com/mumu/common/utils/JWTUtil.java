/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.common.utils;

import java.util.Date;

import org.apache.commons.lang.time.DateUtils;

import com.alibaba.fastjson.JSON;
import com.mumu.common.expcetion.TokenException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.Data;

/**
 * JWTUtil
 *
 * @author liuzhen
 * @version 1.0.0 2025/3/30 14:21
 */
public class JWTUtil {
    private final static String TOKEN_SECRET = "game_token#$%Abc";
    /** TOKEN有效期 七天 */
    private final static long TOKEN_EXPIRE = DateUtils.MILLIS_PER_DAY * 7;

    public static String getUserToken(String openId, long userId) {
        return getUserToken(openId, userId, 0, "-1");
    }

    public static String getUserToken(String openId, long userId, long playerId, String serverId, String... params) {
        // 使用对称加密算法生成签名
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        TokenBody tokenBody = new TokenBody();
        tokenBody.setOpenId(openId);
        tokenBody.setPlayerId(playerId);
        tokenBody.setUserId(userId);
        tokenBody.setServerId(serverId);
        tokenBody.setParam(params);

        String subject = JSON.toJSONString(tokenBody);
        JwtBuilder builder = Jwts.builder().setId(String.valueOf(nowMillis)).setIssuedAt(now).setSubject(subject)
                .signWith(signatureAlgorithm, TOKEN_SECRET);
        long expMillis = nowMillis + TOKEN_EXPIRE;
        Date exp = new Date(expMillis);
        builder.setExpiration(exp);
        return builder.compact();
    }

    public static TokenBody getTokenBody(String token) throws TokenException {
        try {
            Claims claims = Jwts.parser().setSigningKey(TOKEN_SECRET).parseClaimsJws(token).getBody();
            String subject = claims.getSubject();
            TokenBody tokenBody = JSON.parseObject(subject, TokenBody.class);
            return tokenBody;
        } catch (Throwable e) {
            TokenException exp = new TokenException("token解析失败", e);
            if (e instanceof ExpiredJwtException) {
                exp.setExpire(true);
            }
            throw exp;
        }
    }

    @Data
    public static class TokenBody {
        private String openId;
        private long userId;
        private long playerId;
        private String serverId = "1";
        /** 其它的额外参数 */
        private String[] param;
    }
}
