/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.log;

/**
 * LogAction
 * 日志Action模块
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:07
 */
public interface LogAction {
    /** 活动任务模块 */
    String ACTIVITY_TASK = "activityTask";
    /**  */
    String JVM_LOCK = "jvmLock";
    /** GM模块 */
    String GM = "GM";
    /** rpc */
    String RPC = "easyRpc";
    /** 请求rpc */
    String REQUEST_RPC = "requestRpc";
    /** 网络日志 */
    String NET = "serverNet";

    /** 版本号处理 */
    String VERSION = "version";

    /** model初始化 */
    String MODEL_INIT = "modelInit";
    /** model加载 */
    String MODEL_LOAD = "modelLoad";
    /** model存储 */
    String MODEL_SAVE = "modelSave";
    /** model卸载 */
    String MODEL_UNINSTALL = "modelUninstall";
    /** model打包（打包后，可能直接持久化，可能发送给master持久化） */
    String MODEL_PACKAGE = "modelPackage";

    /** 道具变动 */
    String ITEM_CHANGE = "itemChange";
    /** 道具变动事件 */
    String ITEM_CHANGE_EVENT = "itemChangeEvent";


    /**  */
    String RANK = "rank";
}
