package com.mumu.game.mail;

import lombok.Data;

/** 邮件VO对象 @Date: 2025/4/18 下午6:22 @Author: xu.hai */
@Data
public class MailPublishVO {
  /** 邮件信息 */
  private MailParams mailParams;

  /** 目标玩家 */
  private Long playerId;

  public static MailPublishVO of(MailParams params, Long playerId) {
    MailPublishVO publishVO = new MailPublishVO();
    publishVO.setMailParams(params);
    publishVO.setPlayerId(playerId);
    return publishVO;
  }
}
