package com.mumu.game.mail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.DateUtils;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;
import com.game.business.language.enums.MailLanguageEnum;
import com.game.business.mail.consts.MailConstants;
import com.game.business.mail.consts.MailType;
import com.game.business.mail.manager.MailManager;
import com.game.business.player.bean.Player;
import com.game.framework.core.snow.SnowflakeID;

import lombok.Data;

/** 邮件信息构建类 @Date: 2024/9/2 下午4:11 @Author: xu.hai */
@Data
@ProtobufClass
public class MailParams {
  /** 邮件id */
  private Long mailId;

  /** 邮件类型 */
  private int mailType;

  /** 邮件国际化枚举 */
  private MailLanguageEnum mailLanguageEnum;

  /** TODO 邮件国际化枚举字符串 */
  private String mailLanguageEnumSuffixName;

  /** 内容参数 */
  private List<String> contentArgsList = new ArrayList<>();

  /** 邮件标题 */
  private String title;

  /** 邮件内容 */
  private String content;

  /** 邮件创建时间 */
  private long createTime = System.currentTimeMillis();

  /** 邮件过期天数 */
  private int expiredDays = MailConstants.MAIL_EXPIRE;

  /** 邮件过期天数 */
  private long expireTime = DateUtils.addDays(new Date(), this.expiredDays).getTime();

  /** 邮件奖励 */
  private String give;

  /** 后台gm邮件操作人 */
  private String operator;

  /** 不能删除 0-能删除 1-不能删除 */
  private int cannotDel;

  /** 排他锁过期时间/秒（不传默认读配置时间） */
  private transient int lockExpire = MailConstants.MAIL_LOCK_EXPIRE;

  /** 排他锁的 key（不传则不会创建排它锁） */
  private transient Object[] lockKeys;

  public static MailParams build() {
    MailParams params = new MailParams();
    params.setMailId(SnowflakeID.nextId());
    return params;
  }

  public MailParams setMailType(int mailType) {
    this.mailType = mailType;
    return this;
  }

  public MailParams setMailLanguageEnum(MailLanguageEnum mailLanguageEnum) {
    this.mailLanguageEnum = mailLanguageEnum;
    return setMailType(mailLanguageEnum.getType());
  }

  public MailParams setMailLanguageEnumSuffixName(String mailLanguageEnumSuffixName) {
    this.mailLanguageEnumSuffixName = mailLanguageEnumSuffixName;
    return this;
  }

  /** 追加参数 */
  public MailParams appendAgr(Object... args) {
    for (Object arg : args) {
      this.contentArgsList.add(String.valueOf(arg));
    }
    return this;
  }

  public MailParams setTitle(String title) {
    this.title = title;
    return this;
  }

  public MailParams setContent(String content) {
    this.content = content;
    return this;
  }

  public MailParams setExpiredDays(int expiredDays) {
    this.expiredDays = expiredDays <= 0 ? MailConstants.MAIL_EXPIRE : expiredDays;
    this.expireTime = DateUtils.addDays(new Date(), this.expiredDays).getTime();
    return this;
  }

  public MailParams setGive(String give) {
    this.give = give;
    return this;
  }

  public MailParams setOperator(String operator) {
    this.operator = operator;
    return this;
  }

  public MailParams setCannotDel(int cannotDel) {
    this.cannotDel = cannotDel;
    return this;
  }

  public MailParams setLockExpire(int lockExpire) {
    this.lockExpire = lockExpire;
    return this;
  }

  public MailParams setLockKeys(Object... lockKeys) {
    this.lockKeys = lockKeys;
    return this;
  }

  /** 发送玩家邮件 */
  public void sendMail(Player player) {
    sendMail(player.getPlayerId());
  }

  /** 发送玩家邮件 */
  public void sendMail(long playerId) {
    MailManager.self().sendMail(this, playerId);
  }

  /** 发送系统邮件（广播） */
  public void sendSysMail() {
    MailManager.self().sendMail(setMailType(MailType.SYSTEM), null);
  }
}
