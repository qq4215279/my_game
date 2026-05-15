package com.mumu.game.mail.listener;

import org.springframework.stereotype.Component;

import com.game.business.mail.data.MailPublishVO;
import com.game.business.player.manager.PlayerManager;
import com.game.framework.core.redis.channel.RedisChannelListener;
import com.game.framework.core.redis.consts.RedisChannel;
import com.game.framework.net.server.MessageSender;
import com.game.mail.service.MailService;

import jakarta.annotation.Resource;

/** 邮件监听 @Date: 2024/11/4 下午2:56 @Author: xu.hai */
@Component
public class WorldMailListener implements RedisChannelListener<MailPublishVO> {
  @Resource PlayerManager playerManager;
  @Resource MailService mailService;

  @Override
  public void onMessage(String channel, MailPublishVO mailVo) {
    Long playerId = mailVo.getPlayerId();
    if (playerId == null) {
      playerManager.forPlayers(
          id ->
              MessageSender.sendRunNow(
                  id,
                  () -> mailService.saveSysMail(id, mailVo.getMailParams()),
                  "receiveSysMail",
                  id));
    } else {
      MessageSender.sendRunNow(
          playerId,
          () -> mailService.savePlayerMail(playerId, mailVo.getMailParams()),
          "receiveMail",
          playerId);
    }
  }

  @Override
  public RedisChannel subscribeChannel() {
    return RedisChannel.MAIL;
  }
}
