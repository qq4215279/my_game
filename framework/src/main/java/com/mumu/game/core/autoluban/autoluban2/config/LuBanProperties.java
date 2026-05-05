package com.mumu.game.core.autoluban.autoluban2.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 鲁班配置实体 @Date: 2021/7/8 16:47 @Author: xu.hai */
@Data
@ConfigurationProperties(prefix = LuBanProperties.LUBAN_PREFIX)
public class LuBanProperties {

  public static final String LUBAN_PREFIX = "luban";

  /** 是否开启luban配置 */
  private boolean enable;

  /** 鲁班服务端口 */
  private int port;

  /** 是否拉取远程配置 false【加载本地仓库configFileCachePath目录下配置】; true【拉取远程仓库endpoint服务器上的配置】 */
  private boolean remote;

  /** 是否打印日志 */
  private boolean log;

  /** 需要加载哪些包下的配置class */
  private String[] configEntityPackageNames;

  /** 本地配置仓库地址 */
  private String configFileCachePath;

  /** 远程配置仓库地址 */
  private String endpoint;

  /** 拉取远程配置仓库的秘钥 */
  private String secretAccessKey;
}
