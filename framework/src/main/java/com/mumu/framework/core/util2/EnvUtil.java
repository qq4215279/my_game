/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.util2;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 环境工具 @Date: 2025/2/19 下午2:32 @Author: xu.hai */
@Component
public class EnvUtil {
  /** 本地环境 - local */
  public static final String ENV_LOCAL = "local";

  /** 开发环境 - dev */
  public static final String ENV_DEV = "dev";

  /** 测试环境 - TEST */
  public static final String ENV_TEST = "test";

  /** 正式环境 - prod */
  public static final String ENV_PROD = "prod";

  /** 激活的服务器环境 */
  @Getter private static String activeEnv;

  @Value("${spring.profiles.active}")
  public void setEvn(String env) {
    EnvUtil.activeEnv = env;
  }

  /** 是正式环境 */
  public static boolean isProd() {
    return ENV_PROD.equals(activeEnv);
  }

  /** 非正式环境 */
  public static boolean notProd() {
    return !isProd();
  }
}
