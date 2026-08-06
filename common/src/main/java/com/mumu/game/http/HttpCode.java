package com.mumu.game.http;

/**
 * HttpCode
 * http错误码
 *
 * @author liuzhen
 * @version 1.0.0 2024/11/26 11:36
 */
public interface HttpCode {
    /** 成功 */
    int SUCCESS = 0;
    /** 失败 */
    int FAIL = 1;
    /** 订单不存在 */
    int ORDER_NOT_EXIST = 2;
    /** 订单已标记为失败 */
    int ORDER_HAS_MARK_FAIL = 3;
    /** 玩家不存在 */
    int PLAYER_NOT_EXIST = 4;

    /** 验证码失败 */
    int VERIFICATION_ERROR = 98;
    /** 错误请求 */
    int BAD_REQUEST = 400;
    /** 请求过多 */
    int TOO_MANY_REQUESTS = 429;
    /** 服务器异常 */
    int SERVER_EXCEPTION = 500;

}
