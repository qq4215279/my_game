package com.mumu.game.mail;

/** 邮件配置 @Date: 2024/9/3 下午2:44 @Author: xu.hai */
public interface MailConstants {

  /** 邮件保留天 */
  int MAIL_EXPIRE = 30;

  /** 邮件锁过期时间 秒 */
  int MAIL_LOCK_EXPIRE = 3600;

  /** 邮件批量拉取数量 */
  int MAIL_BATCH_PULL = 100;

  /** 邮件查看的分页获取数量 */
  int MAIL_GET_PAGE_SIZE = 10;

  /** 已删除 */
  byte MAIL_FLAG_DELETED = -1;

  /** 未读 */
  byte MAIL_FLAG_UNREAD = 0;

  /** 已读 */
  byte MAIL_FLAG_READ = 1;

  /** 已领取 */
  byte MAIL_FLAG_REWARD = 2;
}
