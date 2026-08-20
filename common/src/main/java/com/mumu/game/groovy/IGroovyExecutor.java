package com.mumu.game.groovy;

/** Groovy执行器 @Date: 2024/12/18 下午8:07 @Author: xu.hai */
/**
 * IGroovyExecutor
 * Groovy执行器
 * @author liuzhen
 * @version 1.0.0 2026/8/20 09:34
 */
public interface IGroovyExecutor {

  /** 执行脚本 */
  Object execute(String... params) throws Exception;
}
