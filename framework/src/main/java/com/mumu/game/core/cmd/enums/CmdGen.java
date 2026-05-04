package com.mumu.game.core.cmd.enums;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * CmdGen
 * Cmd协议messageId生成工具类
 * 功能：
 * 1. 为Cmd枚举中的每个协议生成唯一的6位数messageId（范围：100000-999999）
 * 2. 支持增量更新：已存在的协议messageId保持不变，只为新增协议生成新ID
 * 3. 将生成的配置保存到framework/src/main/resources/cmd/cmd.json文件
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
     * 生成cmd.json文件
     * 如果文件已存在，则读取已有配置，只为新增的Cmd枚举生成新的messageId
     * 确保所有messageId都是唯一的6位数
     * JSON格式：{协议名: {messageId: xxx, reqMessage: "xxx", resMessage: "xxx"}}
     * 如果reqMsgClass或resMsgClass为null，则对应字段为空字符串
     *
     * @throws IOException IO异常
     */
    public static void generateMessageIdFile() throws IOException {
        Map<String, ProtocolInfo> protocolMap = new HashMap<>();
        Random random = new Random();

        String outputDir = "framework/src/main/resources/cmd";
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File outputFile = new File(outputDir + "/cmd.json");

        // 读取已存在的配置文件，保留已有的messageId
        if (outputFile.exists()) {
            ObjectMapper readMapper = new ObjectMapper();
            Map<String, ProtocolInfo> existingMap = readMapper.readValue(outputFile,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, ProtocolInfo>>() {});
            protocolMap.putAll(existingMap);
            System.out.println("读取到已存在的配置，共 " + existingMap.size() + " 个协议");
        }

        // 遍历所有Cmd枚举，为没有messageId的新增协议生成新的ID
        for (Cmd cmd : Cmd.values()) {
            // 跳过None空协议
            if (cmd == Cmd.None) {
                continue;
            }

            // 如果该协议已有配置，则跳过（保持原有值不变）
            if (protocolMap.containsKey(cmd.name())) {
                continue;
            }

            // 生成不重复的6位数随机messageId
            int messageId = 0;
            int finalMessageId = messageId;
            do {
                messageId = random.nextInt(MAX_ID - MIN_ID + 1) + MIN_ID;
            } while (protocolMap.values().stream().anyMatch(p -> p.getMessageId() == finalMessageId));

            // 获取请求和响应消息类名，如果为null则使用空字符串
            String reqMessage = cmd.getReqMsgClass() != null ? cmd.getReqMsgClass().getSimpleName() : "";
            String resMessage = cmd.getResMsgClass() != null ? cmd.getResMsgClass().getSimpleName() : "";

            ProtocolInfo protocolInfo = new ProtocolInfo(messageId, reqMessage, resMessage);
            protocolMap.put(cmd.name(), protocolInfo);
            System.out.println("新增协议: " + cmd.name() + " -> " + protocolInfo);
        }

        // 将最终的配置写入JSON文件
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();

        protocolMap.forEach((key, value) -> {
            ObjectNode protocolNode = mapper.createObjectNode();
            protocolNode.put("messageId", value.getMessageId());
            protocolNode.put("reqMessage", value.getReqMessage());
            protocolNode.put("resMessage", value.getResMessage());
            rootNode.set(key, protocolNode);
        });

        mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, rootNode);

        System.out.println("\nMessageId生成完成，共 " + protocolMap.size() + " 个协议");
        System.out.println("文件路径: " + outputFile.getAbsolutePath());
        System.out.println("\n所有协议的MessageId:");
        protocolMap.forEach((key, value) -> System.out.println(key + ": " + value));
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
