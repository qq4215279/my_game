package com.mumu.game.core.cmd.anno;

import com.mumu.game.core.cmd.enums.Cmd;
import com.mumu.game.core.cmd.enums.RpcCmd;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RpcCmdMapping
 * rpc 协议注解
 * @author liuzhen
 * @version 1.0.0 2026/5/5 14:53
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RpcCmdMapping {
    /** rpcCmd */
    RpcCmd value();
}
