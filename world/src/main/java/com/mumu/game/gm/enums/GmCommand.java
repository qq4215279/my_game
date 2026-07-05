package com.mumu.game.gm.enums;

import com.mumu.game.core.net.consts.ServiceType;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * GmCommand
 * gm命令列表
 * @author liuzhen
 * @version 1.0.0 2026/7/5 20:16
 */
@Getter
public enum GmCommand {
    /** 帮助 */
    HELP("help", "获取所有gm指令", "key=help", 0),
    /** 道具 */
    ADD_ITEM("addItem", "增加道具# arg0: 掉落字符串", "key=addItem&playerId=10000001&args=22000,2000", 0),
    /** 所有道具 */
    ADD_ALL_ITEM("addAllItem", "增加所有道具#不指定数量默认300000# arg0: 道具数量", "key=addAllItem&playerId=10000001&args=10000000", 0),


    ;


    GmCommand(String key, String desc, String example, int count) {
        this.key = key;
        this.desc = desc;
        this.example = example;
        this.exeServiceType = ServiceType.WORLD;
        this.count = count;
    }

    GmCommand(String key, String desc, String example, int count, ServiceType exeServiceType) {
        this.key = key;
        this.desc = desc;
        this.example = example;
        this.count = count;
        this.exeServiceType = exeServiceType;
    }

    /** 指令key */
    private final String key;
    /** 描述 */
    private final String desc;
    /** 示例 */
    private final String example;
    /** 参数数量 */
    private final int count;
    /** 服务组 默认WORLD */
    private final ServiceType exeServiceType;

    /** name 与 GmCmdEnum 映射 */
    private static final Map<String, GmCommand> NAME_ENUM_MAP = new HashMap<>();
    static {
        for (GmCommand gmCommand :values()) {
            NAME_ENUM_MAP.put(gmCommand.key, gmCommand);
        }
    }

    /**
     * 获取指令
     * @param name name
     * @return com.game.gm.common.GmCmdEnum
     */
    public static GmCommand getGmCmdEnum(String name) {
        return NAME_ENUM_MAP.get(name);
    }


    public static void printAllCommand() {
        for (GmCommand gmCommand : values()) {
            System.out.println("指令key: " + gmCommand.key + " 描述: " + gmCommand.desc + " 示例: " + gmCommand.example);
        }
    }

    public static void main(String[] args) {
        printAllCommand();
    }

}

