/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.cmd.enums;


import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import lombok.Data;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;

/**
 * CmdManager
 * Cmd管理器
 * Cmd管理器，负责管理协议Cmd与messageId的映射关系
 * 从cmd.json文件中读取协议配置并初始化映射表
 * @author liuzhen
 * @version 1.0.0 2025/3/30 13:22
 */
@Component
public class CmdManager {
    @Getter
    private static Map<Integer, Cmd> messageIdCmdMap = new HashMap<>();

    @Getter
    private static Map<Cmd, Integer> cmdReqMessageIdMap = new HashMap<>();

    @Getter
    private static Map<Cmd, Integer> cmdResMessageIdMap = new HashMap<>();

    static {
        try {
            loadCmdConfig();
        } catch (Exception e) {
            throw new RuntimeException("加载cmd.json配置文件失败", e);
        }
    }

    /**
     * 从cmd.json文件加载协议配置
     * 读取文件中的协议信息，初始化messageId与Cmd的映射关系
     *
     * @throws Exception 加载异常
     */
    private static void loadCmdConfig() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // 从classpath读取cmd.json文件
        InputStream inputStream = CmdManager.class.getClassLoader().getResourceAsStream("cmd/cmd.json");
        if (inputStream == null) {
            throw new IllegalStateException("无法找到cmd/cmd.json配置文件");
        }

        // 解析JSON文件，读取协议配置
        Map<String, ProtocolInfo> protocolConfigMap = mapper.readValue(inputStream, new TypeReference<>() {});

        // 遍历配置，初始化映射表
        for (Map.Entry<String, ProtocolInfo> entry : protocolConfigMap.entrySet()) {
            String cmdName = entry.getKey();
            ProtocolInfo protocolInfo = entry.getValue();

            try {
                // 根据协议名称获取对应的Cmd枚举
                Cmd cmd = Cmd.valueOf(cmdName);

                int messageId = protocolInfo.getMessageId();

                // 初始化请求messageId映射
                if (protocolInfo.getReqMessage() != null && !protocolInfo.getReqMessage().isEmpty()) {
                    messageIdCmdMap.put(messageId, cmd);
                    cmdReqMessageIdMap.put(cmd, messageId);
                }

                // 初始化响应messageId映射（使用messageId + 1）
                if (protocolInfo.getResMessage() != null && !protocolInfo.getResMessage().isEmpty()) {
                    int resMessageId = messageId + 1;
                    messageIdCmdMap.put(resMessageId, cmd);
                    cmdResMessageIdMap.put(cmd, resMessageId);
                }
            } catch (IllegalArgumentException e) {
                // 如果Cmd枚举中不存在该协议名，记录警告但继续处理其他协议
                System.err.println("警告: Cmd枚举中不存在协议: " + cmdName);
            }
        }

        System.out.println("CmdManager初始化完成，共加载 " + protocolConfigMap.size() + " 个协议配置");
    }

    /**
     * 协议信息内部类
     * 对应cmd.json中的协议配置结构
     */
    @Data
    private static class ProtocolInfo {
        private int messageId;
        private String reqMessage;
        private String resMessage;
    }


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
