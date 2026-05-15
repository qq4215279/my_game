package com.mumu.game.mail.service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.game.business.language.enums.LanguageEnum;
import com.game.business.language.enums.MailLanguageEnum;
import com.game.business.mail.consts.MailConstants;
import com.game.business.mail.data.MailParams;
import com.game.business.mail.domain.PlayerMailDO;
import com.game.business.mail.manager.MailManager;
import com.game.business.mail.operator.MailOperator;
import com.game.business.player.bean.Player;
import com.game.business.player.event.world.OnlineEvent;
import com.game.business.player.manager.PlayerManager;
import com.game.consts.Symbol;
import com.game.framework.core.log.LogTopic;
import com.game.framework.core.redis.RedisUtil;
import com.game.framework.core.redis.consts.RedisKey;
import com.game.framework.core.redis.consts.RedisRank;
import com.game.framework.core.utils.CovertUtil;
import com.game.mail.operator.WorldMailIdxOperator;
import com.game.system.config.ConfigSystemParamsEnum;
import com.game.template.func.core.FunctionIdEnum;
import com.game.util.JsonUtil;
import com.google.common.collect.Sets;

import cn.hutool.core.lang.Pair;
import jakarta.annotation.Resource;

/** 邮件服务 @Date: 2024/9/2 下午5:29 @Author: xu.hai */
@Service
public class MailService {
  @Resource PlayerManager playerManager;
  @Resource MailManager mailManager;
  @Resource MailOperator mailOperator;
  @Resource WorldMailIdxOperator worldMailIdxOperator;

  @EventListener(OnlineEvent.class)
  public void online(OnlineEvent event) {
    Player player = event.getPlayer();
    pullAndSaveMails(player.getPlayerId());
    // 红点推送
    FunctionIdEnum.WORLD_MAIL.pushFunctionStateMessage(player.getPlayerId());
  }

  /** 拉取新的邮件信息保存到玩家DO */
  private void pullAndSaveMails(long playerId) {
    // 拉取玩家邮件信息，并删除 Redis缓存
    Set<String> mailIds = Sets.newHashSet();
    Collection<MailParams> playerMails = pullPlayerMails(playerId);
    for (MailParams mailParams : playerMails) {
      saveMail(playerId, mailParams);
      mailIds.add(String.valueOf(mailParams.getMailId()));
    }
    RedisUtil.hdel(RedisKey.MAIL_PLAYER.buildKey(playerId), mailIds);

    // 拉取系统邮件信息，并更新系统邮件索引
    Collection<MailParams> systemMails = pullSysMails(playerId);
    for (MailParams mailParams : systemMails) {
      saveMail(playerId, mailParams);
      worldMailIdxOperator.updateSysMailIdx(playerId, mailParams.getMailId());
    }

    LogTopic.ACTION.info(
        playerId,
        "pullAndSaveMails",
        "playerMails",
        mailIds.size(),
        "systemMails",
        systemMails.size());
  }

  /** 拉取玩家个人邮件信息 */
  private Collection<MailParams> pullPlayerMails(long playerId) {
    return RedisUtil.batchHGetValues(
        RedisKey.MAIL_PLAYER.buildKey(playerId),
        Symbol.PATTERN,
        MailConstants.MAIL_BATCH_PULL,
        json -> JsonUtil.fromJson(json, MailParams.class));
  }

  /** 拉取玩家系统邮件 */
  private Collection<MailParams> pullSysMails(long playerId) {
    long idx = worldMailIdxOperator.getSysMailIdx(playerId);
    // Set<String> mailIds = RedisUtil.getElementsAboveScore(RedisKey.MAIL_SYSTEM_IDS.buildKey(),
    // idx);
    Set<String> mailIds = RedisRank.SYSTEM_MAIL.getElementsAboveScore(idx);
    return mailIds.stream()
        .map(Long::parseLong)
        .filter(mailId -> mailId != idx)
        .map(mailId -> RedisUtil.get(RedisKey.MAIL_SYSTEM.buildKey(mailId), MailParams.class))
        // todo 获取不到系统邮件时移除此ID
        .filter(Objects::nonNull)
        .toList();
    // 更新索引值
    // MailBean last = CollectionUtil.getLast(mails);
    // if (last != null) {
    //   worldMailIdxOperator.updateSysMailIdx(playerId, last.getMailId());
    // }
  }

  /** 保存邮件信息 */
  public void saveMail(long playerId, MailParams mailParams) {
    PlayerMailDO mailDO = mailOperator.getMail(playerId, mailParams.getMailId());
    if (mailDO != null) {
      LogTopic.ACTION.warn(
          playerId, "saveMail mailId already exist", "mailBean", mailParams, "mailDO", mailDO);
      return;
    }
    saveMailBeforeCheckMailCapacity(playerId);

    mailDO = new PlayerMailDO();
    mailDO.setPlayerId(playerId);
    mailDO.setMailId(mailParams.getMailId());
    mailDO.setMailType(mailParams.getMailType());
    // 填充邮件标题与内容
    Pair<String, String> mailPair = getMailTitleAndContent(playerId, mailParams);
    mailDO.setTitle(mailPair.getKey());
    mailDO.setContent(mailPair.getValue());
    mailDO.setExpireTime(mailParams.getExpireTime());
    mailDO.setCreateTime(mailParams.getCreateTime());
    mailDO.setRewards(mailParams.getGive() == null ? "" : mailParams.getGive());
    mailDO.setCannotDel(mailParams.getCannotDel());
    mailOperator.insert(mailDO);
  }

  /** 保存邮件前检查邮件容量，超出移除 */
  private void saveMailBeforeCheckMailCapacity(long playerId) {
    List<PlayerMailDO> activeMails = mailOperator.getActiveMails(playerId);
    if (activeMails.size() >= ConfigSystemParamsEnum.MAIL_LIMIT.getInt())
      mailOperator.delete(activeMails.get(0));
  }

  /** 解析邮件标题跟内容 */
  private Pair<String, String> getMailTitleAndContent(long playerId, MailParams mailParams) {
    // 填充邮件标题与内容
    String title = mailParams.getTitle();
    String content = mailParams.getContent();
    MailLanguageEnum mailLanguageEnum = mailParams.getMailLanguageEnum();
    // 1.
    if (mailLanguageEnum != null) {
      title = LanguageEnum.getContent(playerId, mailLanguageEnum.getTitleKey());
      content =
          CovertUtil.indexedFormat(
              LanguageEnum.getContent(playerId, mailLanguageEnum.getContentKey()),
              mailParams.getContentArgsList().toArray());

      // 2. TODO
    } else {
      String mailLanguageEnumSuffixName = mailParams.getMailLanguageEnumSuffixName();
      if (StringUtils.isNotEmpty(mailLanguageEnumSuffixName)) {
        title = LanguageEnum.getContent(playerId, MailLanguageEnum.getTitleKey(mailLanguageEnumSuffixName));
        content = CovertUtil.indexedFormat(
                LanguageEnum.getContent(playerId, MailLanguageEnum.getContentKey(mailLanguageEnumSuffixName)),
                mailParams.getContentArgsList().toArray());
      }
    }


    return Pair.of(title, content);
  }

  // ============================================================================>

  /** 保存玩家邮件 */
  public void savePlayerMail(long playerId, MailParams mailParams) {
    if (playerManager.inServer(playerId)) {
      saveMail(playerId, mailParams);
      // 红点推送
      FunctionIdEnum.WORLD_MAIL.pushFunctionStateMessage(playerId);
      return;
    }
    // 玩家可能已经离线，直接推送到redis
    mailManager.pushMailToRedis(playerId, mailParams);
    LogTopic.ACTION.info("saveMail 玩家已离线，直接写入Redis", "playerId", playerId, "mail", mailParams);
  }

  /** 保存系统邮件 */
  public void saveSysMail(long playerId, MailParams mailParams) {
    saveMail(playerId, mailParams);
    worldMailIdxOperator.updateSysMailIdx(playerId, mailParams.getMailId());
    // 红点推送
    FunctionIdEnum.WORLD_MAIL.pushFunctionStateMessage(playerId);
  }

  /** 尝试删除系统邮件 */
  public void tryDelSysMail(List<Long> ids) {
    for (long id : ids) {
      // 移除邮件ID集合
      RedisRank.SYSTEM_MAIL.zremove(id);
      // 移除邮件详情
      RedisUtil.del(RedisKey.MAIL_SYSTEM.buildKey(id));
    }
  }
}
