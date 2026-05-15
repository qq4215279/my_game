package com.mumu.game.mail.manager;

import org.springframework.stereotype.Component;

import com.game.business.mail.consts.MailType;
import com.game.business.mail.data.MailParams;
import com.game.business.mail.data.MailPublishVO;
import com.game.business.player.manager.PlayerManager;
import com.game.consts.Symbol;
import com.game.framework.core.consts.ConfigSwitchEnum;
import com.game.framework.core.log.LogTopic;
import com.game.framework.core.redis.RedisUtil;
import com.game.framework.core.redis.consts.RedisChannel;
import com.game.framework.core.redis.consts.RedisKey;
import com.game.framework.core.redis.consts.RedisRank;
import com.game.framework.core.utils.SpringContextUtils;
import com.game.framework.core.utils.TimeUtil;
import com.game.framework.net.consts.ServerGroup;
import com.game.util.JsonUtil;

import cn.hutool.core.util.ArrayUtil;
import jakarta.annotation.Resource;

/** 邮件管理类 @Date: 2024/9/2 下午4:05 @Author: xu.hai */
@Component
public class MailManager {
  public static MailManager self() {
    return SpringContextUtils.getBean(MailManager.class);
  }

  @Resource PlayerManager playerManager;

  /**
   * 发送邮件
   * @param mailParams  邮件信息
   * @param playerId  目标玩家，null表示系统邮件
   */
  public void sendMail(MailParams mailParams, Long playerId) {
    try {
      LogTopic.ACTION.debug(
          ConfigSwitchEnum.LOG_MAIL,
          "sendMail",
          "playerId",
          playerId,
          "mailParams",
          JsonUtil.toJson(mailParams));
      if (!tryLock(mailParams)) {
        LogTopic.ACTION.info(
            "sendMail lock failed", "playerId", playerId, "mailParams", mailParams);
        return;
      }
      // 1.判断邮件类型
      if (playerId == null) {
        // 无目标玩家，标记为系统邮件, 写入Redis（用于不在线玩家接收）
        mailParams.setMailType(MailType.SYSTEM);
        pushMailToRedis(mailParams);
      } else if (playerManager.getServerId(playerId, ServerGroup.WORLD) == 0) {
        // 发给指定玩家，若玩家不在线，写入Redis
        pushMailToRedis(playerId, mailParams);
        return;
      }
      // 2.发布邮件（用于在线玩家接收）
      RedisChannel.MAIL.publish(MailPublishVO.of(mailParams, playerId));
    } catch (Exception e) {
      LogTopic.ACTION.error(e, "sendMail error", "playerId", playerId, "mailParams", mailParams);
    }
  }

  /** 推送玩家邮件到 Redis存储 */
  public void pushMailToRedis(long playerId, MailParams mailParams) {
    long expired =
        (mailParams.getExpireTime() - System.currentTimeMillis()) / TimeUtil.ONE_SECOND_MILLIS;
    RedisUtil.hset(
        RedisKey.MAIL_PLAYER.buildKey(playerId),
        String.valueOf(mailParams.getMailId()),
        mailParams,
        expired);
  }

  /** 推送系统到 Redis存储 */
  private void pushMailToRedis(MailParams mailParams) {
    long expired =
        (mailParams.getExpireTime() - System.currentTimeMillis()) / TimeUtil.ONE_SECOND_MILLIS;
    // 记录邮件详情
    RedisUtil.set(RedisKey.MAIL_SYSTEM.buildKey(mailParams.getMailId()), mailParams, expired);
    // 记录邮件ID集合
    RedisRank.SYSTEM_MAIL.zadd(mailParams.getMailId(), mailParams.getMailId());
    // RedisUtil.zadd(RedisKey.MAIL_SYSTEM_IDS.buildKey(), mail.getMailId(), mail.getMailId());
  }

  /** 尝试上锁（灰度期间避免邮件重复发放） */
  private boolean tryLock(MailParams mailParams) {
    Object[] lockKeys = mailParams.getLockKeys();
    if (ArrayUtil.isEmpty(lockKeys)) {
      return true;
    }
    String lockKey =
        RedisKey.MAIL_LOCK.buildKey(ArrayUtil.join(lockKeys, Symbol.UNDERLINE));
    return RedisUtil.tryLock(lockKey, mailParams.getLockExpire());
  }
}
