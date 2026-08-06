package com.mumu.game.account.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * AccountEntity
 * 账户信息
 * @author liuzhen
 * @version 1.0.0 2026/8/2 14:10
 */
@Data
@Document("account_info")
public class AccountEntity {
  /** 玩家唯一ID */
  @Id
  private Long id;

  /** 注册的设备号 */
  private String deviceId;

  /** 注册的渠道号 */
  private String channel;

  /** 手机号码 */
  private String telephone;

  /** 邮箱 */
  private String email;

  /** 国家 */
  private String country;

  /** ip */
  private String ip;

  /** 创建时间 */
  private Date createTime;

  /** 更新时间 */
  private Date updateTime;

  /** 申请注销的时间ms */
  private long deactivateTime;

}
