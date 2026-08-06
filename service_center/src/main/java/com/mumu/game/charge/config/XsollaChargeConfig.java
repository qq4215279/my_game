package com.mumu.game.charge.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * XsollaChargeConfig
 * 艾克索拉（第三方支付）充值配置
 * @author liuzhen
 * @version 1.0.0 2025/6/10 14:13
 */
@Data
@Component
@Configuration
public class XsollaChargeConfig {
  /** secretKey */
  @Value("${charge.xsolla.secret_key}")
  private String secretKey = "";

  /** 项目id */
  @Value("${charge.xsolla.project_id}")
  private int projectId = 0;

  /** 商户id */
  @Value("${charge.xsolla.merchant_id}")
  private int merchantId = 0;
}
