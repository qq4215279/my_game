/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.cmd.enums;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

/**
 * CmdManager
 * Cmd管理器 TODO
 * @author liuzhen
 * @version 1.0.0 2025/3/30 13:22
 */
public class CmdManager {
    @Getter
    private static Map<Integer, Cmd> messageIdCmdMap = new HashMap<>();

    @Getter
    private static Map<Cmd, Integer> cmdReqMessageIdMap = new HashMap<>();

    @Getter
    private static Map<Cmd, Integer> cmdResMessageIdMap = new HashMap<>();

    /**
     * 请求获取Cmd
     * @param messageId messageId
     * @since 2025/3/30 13:37
     * @return com.mumu.framework.core.cmd.enums.Cmd
     */
    public static Cmd getCmd(int messageId) {
        return messageIdCmdMap.get(messageId);
    }

    /**
     * 获取请求Cmd对应messageId
     * @param cmd cmd
     * @return int
     * @since 2025/3/30 13:37
     */
    public static int getReqMessageId(Cmd cmd) {
        return cmdReqMessageIdMap.get(cmd);
    }

    /**
     * 获取相应Cmd对应messageId
     * @param cmd cmd
     * @return int
     * @since 2025/3/30 13:38
     */
    public static int getResMessageId(Cmd cmd) {
        return cmdResMessageIdMap.get(cmd);
    }
}
