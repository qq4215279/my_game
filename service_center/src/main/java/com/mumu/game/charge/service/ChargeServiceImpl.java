package com.mumu.game.charge.service;

import com.mumu.game.charge.consts.ChargeConstants;
import com.mumu.game.charge.dao.ChargeInfoDao;
import com.mumu.game.charge.entity.ChargeInfo;
import com.mumu.game.constants.Symbol;
import com.mumu.game.http.HttpCode;
import com.mumu.game.http.HttpResult;
import com.mumu.game.log.LogTopic;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * ChargeServiceImpl
 *
 * @author liuzhen
 * @version 1.0.0 2026/8/2 13:54
 */
@Service
public class ChargeServiceImpl implements ChargeService {
    /** log */
    private final static LogTopic log = LogTopic.ACTION;

    /** 过期时间 */
    private static long expireTime = TimeUnit.DAYS.toMillis(10);

    /** 客户端弹出消息需要失败次数 */
    private static final int POP_MSG_COUNT_CONDITION = 3;

    @Resource
    private ChargeInfoDao chargeInfoDao;

    @Override
    public HttpResult markOrderFail(String orderId, String errorInfo, int failType) {
        LogTopic.ACTION.info("markOrderFail", "orderId", orderId, "errorInfo", errorInfo, "failType", failType);
        ChargeInfo chargeInfo = chargeInfoDao.getChargeInfo(orderId);
        if (chargeInfo == null) {
            return HttpResult.error(HttpCode.ORDER_NOT_EXIST, "订单不存在");
        }
        int state = chargeInfo.getState();
        if (state == ChargeConstants.CLIENT_FAIL_STATE) {
            return HttpResult.error(HttpCode.ORDER_HAS_MARK_FAIL, "订单已标记为失败");
        }
        if (state != ChargeConstants.INIT_CHARGE_STATE) {
            return HttpResult.error(HttpCode.BAD_REQUEST, "订单状态为非初始状态");
        }

        long playerId = chargeInfo.getPlayerId();
        /*AccountEntity accountEntity = accountDao.getAccountEntity(playerId);
        if (accountEntity == null) {
            return HttpResult.error(HttpCode.PLAYER_NOT_EXIST, "玩家不存在");
        }*/

        // 本期
        /*int period = WeekUtil.offset(0).weekOfYear();
        String key = RedisKey.CHARGE_FAIL_RECORD.buildKey(period);
        int add = failType == FAIL_TYPE.ACTIVE_CANCEL ? 1 : POP_MSG_COUNT_CONDITION;
        int cur = (int) RedisUtil.hincrWithExpire(key, playerId, add, expireTime);
        // 是否弹出信息
        boolean popMsg = cur >= POP_MSG_COUNT_CONDITION && cur - add < POP_MSG_COUNT_CONDITION;*/
        boolean popMsg = false;


        // 标记订单状态
        chargeInfo.setState(ChargeConstants.CLIENT_FAIL_STATE);
        String sourceExtraInfo = chargeInfo.getExtraInfo();
        String newExtraInfo;
        if (!StringUtils.isEmpty(sourceExtraInfo)) {
            newExtraInfo = sourceExtraInfo + Symbol.AMP + errorInfo;
        } else {
            newExtraInfo = errorInfo;
        }
        chargeInfo.setExtraInfo(newExtraInfo);
        chargeInfoDao.save(chargeInfo);


        return HttpResult.success().add("popMsg", popMsg);
    }

    /**
     * 失败类型
     */
    public interface FAIL_TYPE {
        /** 主动取消 */
        int ACTIVE_CANCEL = 1;
        /** 其他失败(pay failed) */
        int DEFAULT_FAIL = 2;
        /** 华为失败 */
        int HUAWEI_FAIL = 3;
    }
}
