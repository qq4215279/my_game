package com.mumu.game.core.cmd.enums;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * CmdGen
 * Cmd和RpcCmd协议messageId生成工具类
 * 功能：
 * 1. 为Cmd和RpcCmd枚举中的每个协议生成唯一的6位数messageId（范围：100000-999999）
 * 2. 支持增量更新：已存在的协议messageId保持不变，只为新增协议生成新ID
 * 3. 将生成的配置保存到framework/src/main/resources/cmd/cmd.json和rpcCmd.json文件
 * 4. 保证两个文件的messageId全局唯一
 *
 * 生成的JSON格式：
 * {
 *   "HeartbeatMsg": {
 *     "messageId": 427261,
 *     "reqMessage": "HeartbeatMsgCE",
 *     "resMessage": "HeartbeatMsgEC"
 *   }
 * }
 *
 * 使用方式：直接运行main方法即可
 *
 * @author liuzhen
 * @version 1.0.0 2026/5/4 20:46
 */
public class CmdGen {
    /** messageId最小值（6位数） */
    private static final int MIN_ID = 100000;
    /** messageId最大值（6位数） */
    private static final int MAX_ID = 999999;

    public static void main(String[] args) throws IOException {
        generateMessageIdFile();
    }

    /**
     * 生成cmd.json和rpcCmd.json文件
     * 如果文件已存在，则读取已有配置，只为新增的枚举生成新的messageId
     * 确保所有messageId在两个文件中都是唯一的6位数
     * JSON格式：{协议名: {messageId: xxx, reqMessage: "xxx", resMessage: "xxx"}}
     * 如果reqMsgClass或resMsgClass为null，则对应字段为空字符串
     *
     * @throws IOException IO异常
     */
    public static void generateMessageIdFile() throws IOException {
        String outputDir = "framework/src/main/resources/cmd";
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File cmdFile = new File(outputDir + "/cmd.json");
        File rpcCmdFile = new File(outputDir + "/rpcCmd.json");

        // 用于追踪所有已使用的messageId，保证全局唯一
        Set<Integer> usedMessageIds = new HashSet<>();

        // 读取已存在的cmd.json配置
        Map<String, ProtocolInfo> cmdProtocolMap = new HashMap<>();
        if (cmdFile.exists()) {
            ObjectMapper readMapper = new ObjectMapper();
            Map<String, ProtocolInfo> existingMap = readMapper.readValue(cmdFile,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, ProtocolInfo>>() {});
            cmdProtocolMap.putAll(existingMap);
            // 收集已使用的messageId
            existingMap.values().forEach(p -> usedMessageIds.add(p.getMessageId()));
            System.out.println("读取到已存在的cmd.json配置，共 " + existingMap.size() + " 个协议");
        }

        // 读取已存在的rpcCmd.json配置
        Map<String, ProtocolInfo> rpcCmdProtocolMap = new HashMap<>();
        if (rpcCmdFile.exists()) {
            ObjectMapper readMapper = new ObjectMapper();
            Map<String, ProtocolInfo> existingMap = readMapper.readValue(rpcCmdFile,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, ProtocolInfo>>() {});
            rpcCmdProtocolMap.putAll(existingMap);
            // 收集已使用的messageId
            existingMap.values().forEach(p -> usedMessageIds.add(p.getMessageId()));
            System.out.println("读取到已存在的rpcCmd.json配置，共 " + existingMap.size() + " 个协议");
        }

        Random random = new Random();

        // 遍历所有Cmd枚举，为没有messageId的新增协议生成新的ID
        for (Cmd cmd : Cmd.values()) {
            // 跳过None空协议
            if (cmd == Cmd.None) {
                continue;
            }

            // 如果该协议已有配置，则跳过（保持原有值不变），但记录到usedMessageIds
            if (cmdProtocolMap.containsKey(cmd.name())) {
                usedMessageIds.add(cmdProtocolMap.get(cmd.name()).getMessageId());
                continue;
            }

            // 生成不重复的6位数随机messageId
            int messageId = generateUniqueId(usedMessageIds, random);

            // 获取请求和响应消息类名，如果为null则使用空字符串
            String reqMessage = cmd.getReqMsgClass() != null ? cmd.getReqMsgClass().getSimpleName() : "";
            String resMessage = cmd.getResMsgClass() != null ? cmd.getResMsgClass().getSimpleName() : "";

            ProtocolInfo protocolInfo = new ProtocolInfo(messageId, reqMessage, resMessage);
            cmdProtocolMap.put(cmd.name(), protocolInfo);
            usedMessageIds.add(messageId);
            System.out.println("新增Cmd协议: " + cmd.name() + " -> " + protocolInfo);
        }

        // 遍历所有RpcCmd枚举，为没有messageId的新增协议生成新的ID
        for (RpcCmd rpcCmd : RpcCmd.values()) {
            // 如果该协议已有配置，则跳过（保持原有值不变），但记录到usedMessageIds
            if (rpcCmdProtocolMap.containsKey(rpcCmd.name())) {
                usedMessageIds.add(rpcCmdProtocolMap.get(rpcCmd.name()).getMessageId());
                continue;
            }

            // 生成不重复的6位数随机messageId
            int messageId = generateUniqueId(usedMessageIds, random);

            // 获取请求和响应消息类名，如果为null则使用空字符串
            String reqMessage = rpcCmd.getReqMsgClass() != null ? rpcCmd.getReqMsgClass().getSimpleName() : "";
            String resMessage = rpcCmd.getResMsgClass() != null ? rpcCmd.getResMsgClass().getSimpleName() : "";

            ProtocolInfo protocolInfo = new ProtocolInfo(messageId, reqMessage, resMessage);
            rpcCmdProtocolMap.put(rpcCmd.name(), protocolInfo);
            usedMessageIds.add(messageId);
            System.out.println("新增RpcCmd协议: " + rpcCmd.name() + " -> " + protocolInfo);
        }

        // 将最终的配置写入JSON文件
        writeJsonFile(cmdFile, cmdProtocolMap);
        writeJsonFile(rpcCmdFile, rpcCmdProtocolMap);

        System.out.println("\nMessageId生成完成：");
        System.out.println("Cmd协议共 " + cmdProtocolMap.size() + " 个");
        System.out.println("RpcCmd协议共 " + rpcCmdProtocolMap.size() + " 个");
        System.out.println("总计使用messageId: " + usedMessageIds.size() + " 个");
        System.out.println("\n文件路径:");
        System.out.println("  cmd.json: " + cmdFile.getAbsolutePath());
        System.out.println("  rpcCmd.json: " + rpcCmdFile.getAbsolutePath());

        System.out.println("\n所有协议的MessageId:");
        System.out.println("=== Cmd ===");
        cmdProtocolMap.forEach((key, value) -> System.out.println(key + ": " + value));
        System.out.println("=== RpcCmd ===");
        rpcCmdProtocolMap.forEach((key, value) -> System.out.println(key + ": " + value));
    }

    /**
     * 生成一个唯一的messageId
     *
     * @param usedMessageIds 已使用的messageId集合
     * @param random 随机数生成器
     * @return 唯一的messageId
     */
    private static int generateUniqueId(Set<Integer> usedMessageIds, Random random) {
        int messageId;
        do {
            messageId = random.nextInt(MAX_ID - MIN_ID + 1) + MIN_ID;
        } while (usedMessageIds.contains(messageId));
        return messageId;
    }

    /**
     * 将协议配置写入JSON文件
     *
     * @param file 目标文件
     * @param protocolMap 协议映射表
     * @throws IOException IO异常
     */
    private static void writeJsonFile(File file, Map<String, ProtocolInfo> protocolMap) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();

        protocolMap.forEach((key, value) -> {
            ObjectNode protocolNode = mapper.createObjectNode();
            protocolNode.put("messageId", value.getMessageId());
            protocolNode.put("reqMessage", value.getReqMessage());
            protocolNode.put("resMessage", value.getResMessage());
            rootNode.set(key, protocolNode);
        });

        mapper.writerWithDefaultPrettyPrinter().writeValue(file, rootNode);
    }

    /**
     * 协议信息内部类
     * 包含messageId、请求消息类名、响应消息类名
     */
    static class ProtocolInfo {
        private int messageId;
        private String reqMessage;
        private String resMessage;

        public ProtocolInfo() {
        }

        public ProtocolInfo(int messageId, String reqMessage, String resMessage) {
            this.messageId = messageId;
            this.reqMessage = reqMessage;
            this.resMessage = resMessage;
        }

        public int getMessageId() {
            return messageId;
        }

        public void setMessageId(int messageId) {
            this.messageId = messageId;
        }

        public String getReqMessage() {
            return reqMessage;
        }

        public void setReqMessage(String reqMessage) {
            this.reqMessage = reqMessage;
        }

        public String getResMessage() {
            return resMessage;
        }

        public void setResMessage(String resMessage) {
            this.resMessage = resMessage;
        }

        @Override
        public String toString() {
            return "ProtocolInfo{" +
                    "messageId=" + messageId +
                    ", reqMessage='" + reqMessage + '\'' +
                    ", resMessage='" + resMessage + '\'' +
                    '}';
        }
    }
}
