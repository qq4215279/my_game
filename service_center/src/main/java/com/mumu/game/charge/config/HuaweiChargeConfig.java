package com.mumu.game.charge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * HuaweiChargeConfig
 * 华为充值配置
 * @author liuzhen
 * @version 1.0.0 2024/11/28 10:51
 */
@Data
@Component
public class HuaweiChargeConfig {
  /** 公钥 */
  @Value("${charge.huawei.public-key}")
  private String publicKey = "";
  /** ClientID */
  @Value("${charge.huawei.client-id}")
  private String CLIENT_ID = "";
  /** ClientSecret */
  @Value("${charge.huawei.client-secret}")
  private String CLIENT_SECRET = "";
  /** token url to get the authorization */
  @Value("${charge.huawei.token-url}")
  private String TOKEN_URL = "";
  /** site for telecom carrier */
  @Value("${charge.huawei.tobtoc-site-url}")
  private String TOBTOC_SITE_URL = "";

}
