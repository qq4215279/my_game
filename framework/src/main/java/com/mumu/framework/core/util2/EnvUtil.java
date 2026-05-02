/*
 * Copyright 2020-2025, mumu without 996. All Right Reserved.
 */

package com.mumu.framework.core.util2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

/**
 * EnvUtil 环境工具
 * 
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:59
 */
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
    @Getter
    private static String activeEnv;

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
