/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.common.proto.message.core;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

/**
 * ErrorCode
 * 错误码
 * @author liuzhen
 * @version 1.0.0 2025/3/30 13:09
 */
@ProtobufClass
public enum ErrorCode {
    /** 成功 */
    SUCCESS,
    /** 失败 */
    FAIL,
    /** 非法请求 */
    ILLEGAL,
    /** 未知错误 */
    UNKNOWN,
    /** 超时 */
    TIME_OUT,
    /** 连接异常 */
    SOCKET_ERROR,
    /** 连接关闭 */
    SOCKET_CLOSED,
    /** TOKEN非法 */
    FAIL_TOKEN_ILLEGAL,
    /** TOKEN过期 */
    FAIL_TOKEN_EXPIRE,
    /** 重复连接，可能异地登陆了 */
    FAIL_REPEATED_CONNECT,
    /** token 认证失败 */
    FAIL_TOKEN,
    /** 玩家不在线 */
    FAIL_PLAYER_OFFLINE,
    /** 未知玩家（玩家不存在） */
    FAIL_PLAYER_UNKNOWN,
    /** 未知桌子（桌子不存在） */
    FAIL_TABLE_UNKNOWN,
    /** 进入游戏失败 */
    FAIL_ENTER_GAME,
    /** 游戏不存在 */
    FAIL_GAME_UNKNOWN,
    /** 玩家正在游戏中 */
    FAIL_PLAYER_IN_GAME,
    /** 玩家正在匹配中 */
    FAIL_MATCHING,
    /** 玩家未在匹配中 */
    FAIL_UNMATCH,
    /** 匹配失败 */
    FAIL_MATCH_ERROR,
    /** 匹配超时 */
    FAIL_MATCH_TIMEOUT,
    /** 玩家不存在 */
    FAIL_PLAYER_NOT_EXIST,
    /** 玩家等待操作 */
    FAIL_PLAYER_NO_BIDER,
    /** 指令不存在 */
    FAIL_COMMAND_NOT_EXIST,
    /** 参数错误 */
    FAIL_PARAM_ERROR,
    /** 命令请求超时 */
    FAIL_REQUEST_COMMAND_TIMEOUT,
    /** 无法操作当前指令 */
    FAIL_CAN_NOT_EXE_COMMAND,
    /** 已确认主花色 */
    FAIL_CONFIRM_MAIN_COLOR,
    /** 当前牌不符合出牌规则 */
    FAIL_UN_CONFORM_RULE_POKER,
    /** 金币不足 */
    FAIL_PLAYER_GOLD_NOT_ENOUGH,
    /** 金币溢出 */
    FAIL_PLAYER_GOLD_OVERFLOW,
    /** 密码错误 */
    FAIL_PASSWORD,
    /** 已达购买上限 */
    FAIL_BUY_LIMIT,
    /** 道具不足 */
    FAIL_ITEM_NOT_ENOUGH,
    /** 不满足购买条件 */
    FAIL_CHECK_BUY_CONDITION,
    /** 商品不存在 */
    FAIL_GOODS_NOT_EXIST,
    /** 订单不存在 */
    FAIL_ORDER_NOT_EXIST,
    /** CD冷却中 */
    FAIL_CD_COOLING,
    /** 未拥有表情包 */
    FAIL_NOT_EXIST_EMOTE,
    /** 游戏已结束 */
    FAIL_GAME_OVER,
    /** 道具不存在 */
    FAIL_ITEM_NOT_EXIST,
    /** 已被禁言 */
    FAIL_CHAT_SILENCE,
    /** 消息存在敏感字符 */
    FAIL_MSG_CONTAINS_SENSITIVE_WORDS,
    /** 聊天次数已达上限 */
    FAIL_CHAT_LIMIT,
    /** 不能添加自己为好友 */
    FAIL_SELF_NOT_FRIEND,
    /** 对方在自己黑名单中 */
    FAIL_TARGET_IN_BLACK_LIST,
    /** 对方好友数量已达上限 */
    FAIL_TARGET_FRIEND_NUM_LIMIT,
    /** 好友数量已达上限 */
    FAIL_SELF_FRIEND_NUM_LIMIT,
    /** 好友已存在 */
    FAIL_FRIEND_EXIST,
    /** 申请好友CD中 */
    FAIL_APPLY_FRIEND_CD,
    /** 对方未申请添加好友 */
    FAIL_TARGET_NO_APPLY_FRIEND,
    /** 好友申请列表为空 */
    FAIL_NO_APPLY_FRIEND,
    /** 对方不是你的好友 */
    FAIL_NOT_FRIEND,
    /** 已达黑名单数量上限 */
    FAIL_BLACK_SIZE_LINIT,
    /** 对方不在黑名单列表中 */
    FAIL_NOT_IN_BLACK_LIST,
    /** 对方在黑名单列表中 */
    FAIL_IN_BLACK_LIST,
    /** 您已被对方加入黑名单 */
    FAIL_JOIN_TARGET_BLACK_LIST,
    /** 好友申请已过期 */
    FAIL_APPLY_OVERDUE,
    /** 聊天消息不存在 */
    FAIL_CHAT_INFO_NOT_EXIST,
    /** 聊天消息撤回超时 */
    FAIL_CHAT_WITHDRWA_TIMEOUT,
    /** 已删除聊天消息 */
    FAIL_HAS_DELETE_CHAT_MSG,
    /** 已撤回聊天消息 */
    FAIL_HAS_WITHDRWA_CHAT_MSG,
    /** 购买指定礼包解锁 */
    FAIL_BUY_APPOINT_GOODS_UNLOCK,
    /** 不满足等级条件 */
    FAIL_LEVEL_NOT_ENOUGH,
    /** 不满足VIP等级条件 */
    FAIL_VIP_LEVEL_NOT_ENOUGH,
    /** 对方拒绝添加好友 */
    FAIL_REFUSE_JOIN_FRIEND,
    /** 功能未开放 */
    FAIL_FUNCTION_NOT_OPOEN,
    /** 礼物数量不足 */
    FAIL_GIFT_NOT_ENOUGH,
    /** 没有绑定账号 */
    FAIL_NO_BIND_ACCOUNT,
    /** 任务未完成 */
    FAIL_TASK_NO_FINISH,
    /** 已领取任务奖励 */
    FAIL_HAS_RECEIVE_TASK_REWARD,
    /** 没有可领取的任务奖励 */
    FAIL_NO_TASK_REWARD,
    /** 参数为空 */
    FAIL_PARAM_EMPTY,
    /** VIP已过期 */
    FAIL_VIP_OVERDUE,
    /** 礼包已过期 */
    FAIL_GIFT_OVERDUE,
    /** 已完成标记 */
    FAIL_FINISH_MARK,
    /** 当前不是语音房 */
    FAIL_NOT_VOICE_ROOM,
    /** 游戏seq不同步 */
    FAIL_GAME_NO_SYNC_SEQ,
    /** 今日已签到完成 */
    FAIL_TODAY_HAS_SIGN,
    /** 已领取过奖励 */
    FAIL_HAS_RECEIVE_REWARD,
    /** 已报名 */
    FAIL_HAS_JOIN,
    /** 未报名 */
    FAIL_UN_JOIN,
    /** 不是报名阶段 */
    FAIL_NOT_IN_JOIN_STAGE,
    /** 公告不存在 */
    FAIL_NOTICE_NOT_EXIST,
    /** 玩家正在比赛中 */
    FAIL_PLAYER_IN_TOURNAMENT,
    /** 比赛未开始 */
    FAIL_TOURNAMENT_NOT_MATCHING,
    /** 比赛被淘汰 */
    FAIL_TOURNAMENT_KNOCKOUT,
    /** 不允许登陆 */
    FAIL_DENY_LOGIN,
    /** 停服期间 */
    FAIL_SERVER_CLOSING,
    /** 任务未开启 */
    FAIL_TASK_NOT_OPEN,
    /** 游戏未开始 */
    FAIL_GAME_UN_START,
}
