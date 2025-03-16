package com.mumu.framework.core.mvc.message;

import lombok.Data;

/**
 * GameMessagePackage
 * 游戏消息包
 * @author liuzhen
 * @version 1.0.0 2025/2/24 23:30
 */
@Data
public class GameMessagePackage {
    /** 头信息 */
    private GameMessageHeader header;
    /** 包体数据 */
    private byte[] body;
}
