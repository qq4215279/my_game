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
    private static Map<Integer, ICmd> messageIdCmdMap = new HashMap<>();

    @Getter
    private static Map<ICmd, Integer> cmdReqMessageIdMap = new HashMap<>();

    static {
        try {
            loadCmdConfig();
        } catch (Exception e) {
            throw new RuntimeException("加载cmd.json配置文件失败", e);
        }
    }

    /**
     * 从cmd.json和rpcCmd.json文件加载协议配置
     * 读取文件中的协议信息，初始化messageId与Cmd/RpcCmd的映射关系
     *
     * @throws Exception 加载异常
     */
    private static void loadCmdConfig() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // 加载cmd.json
        InputStream cmdInputStream = CmdManager.class.getClassLoader().getResourceAsStream("cmd/cmd.json");
        if (cmdInputStream == null) {
            throw new IllegalStateException("无法找到cmd/cmd.json配置文件");
        }
        Map<String, ProtocolInfo> cmdProtocolConfigMap = mapper.readValue(cmdInputStream, new TypeReference<>() {});
        loadProtocolConfig(cmdProtocolConfigMap);
        System.out.println("Cmd配置加载完成，共加载 " + cmdProtocolConfigMap.size() + " 个协议配置");

        // 加载rpcCmd.json
        InputStream rpcCmdInputStream = CmdManager.class.getClassLoader().getResourceAsStream("cmd/rpcCmd.json");
        if (rpcCmdInputStream == null) {
            throw new IllegalStateException("无法找到cmd/rpcCmd.json配置文件");
        }
        Map<String, ProtocolInfo> rpcCmdProtocolConfigMap = mapper.readValue(rpcCmdInputStream, new TypeReference<>() {});
        loadProtocolConfig(rpcCmdProtocolConfigMap);
        System.out.println("RpcCmd配置加载完成，共加载 " + rpcCmdProtocolConfigMap.size() + " 个协议配置");

        int totalSize = messageIdCmdMap.size();
        System.out.println("CmdManager初始化完成，共加载 " + totalSize + " 个协议（Cmd + RpcCmd）");
    }

    /**
     * 加载协议配置到映射表中
     * 同时支持Cmd和RpcCmd，统一填充到三个map中
     *
     * @param protocolConfigMap 协议配置映射表
     */
    private static void loadProtocolConfig(Map<String, ProtocolInfo> protocolConfigMap) {
        for (Map.Entry<String, ProtocolInfo> entry : protocolConfigMap.entrySet()) {
            String cmdName = entry.getKey();
            ProtocolInfo protocolInfo = entry.getValue();

            try {
                ICmd cmd;

                // 尝试从Cmd枚举获取
                try {
                    cmd = Cmd.valueOf(cmdName);
                } catch (IllegalArgumentException e) {
                    // 如果Cmd中不存在，尝试从RpcCmd枚举获取
                    cmd = RpcCmd.valueOf(cmdName);
                }

                int messageId = protocolInfo.getMessageId();
                messageIdCmdMap.put(messageId, cmd);
                cmdReqMessageIdMap.put(cmd, messageId);

            } catch (IllegalArgumentException e) {
                // 如果两个枚举中都不存在该协议名，记录警告但继续处理其他协议
                System.err.println("警告: Cmd和RpcCmd枚举中都不存在协议: " + cmdName);
            }
        }
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
    public static ICmd getCmd(int messageId) {
        return messageIdCmdMap.get(messageId);
    }

    /**
     * 获取请求Cmd对应messageId
     * @param cmd cmd
     * @return int
     * @since 2025/3/30 13:37
     */
    public static int getMessageId(ICmd cmd) {
        return cmdReqMessageIdMap.get(cmd);
    }

}
