package com.mumu.game.mail.operator;

import java.util.List;

import org.springframework.stereotype.Component;

import com.game.business.mail.consts.MailConstants;
import com.game.business.mail.domain.PlayerMailDO;
import com.game.framework.core.automodel.entity.IDataOperator;
import com.game.framework.core.automodel.utils.CacheDataApi;

/** 邮件数据访问类 @Date: 2024/9/3 下午2:56 @Author: xu.hai */
@Component
public class MailOperator implements IDataOperator<PlayerMailDO> {

  /** 获取指定邮件 */
  public PlayerMailDO getMail(long playerId, long mailId) {
    return selectOne(playerId, playerId, mailId);
  }

  /** 获取全部邮件 */
  public List<PlayerMailDO> getMails(long playerId) {
    return selectList(playerId, m -> true);
  }

  /** 获取全部有效的邮件 */
  public List<PlayerMailDO> getActiveMails(long playerId) {
    return selectList(
        playerId,
        m ->
            m.getFlag() != MailConstants.MAIL_FLAG_DELETED
                && m.getExpireTime() > System.currentTimeMillis());
  }

  /** 获取邮件数量 */
  public int getMailCount(long playerId) {
    return getActiveMails(playerId).size();
  }

  /** 删除邮件 */
  public void delete(PlayerMailDO mail) {
    CacheDataApi.delete(mail.getPlayerId(), mail);
  }

  /** 批量更新玩家邮件状态 */
  public void updateMailFlag(PlayerMailDO mailDO, byte flag) {
    mailDO.setFlag(flag);
    update(mailDO);
  }
}
