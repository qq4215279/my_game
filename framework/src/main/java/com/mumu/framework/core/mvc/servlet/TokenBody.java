package com.mumu.framework.core.mvc.servlet;

import lombok.Data;

/**
 * TokenBody
 *
 * @author liuzhen
 * @version 1.0.0 2025/3/3 23:03
 */
@Data
public class TokenBody {
    private String openId;
    private long userId;
    private long playerId;
    private String serverId = "1";
    /** 其它的额外参数 */
    private String[] param;
}
