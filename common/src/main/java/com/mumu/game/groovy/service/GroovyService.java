package com.mumu.game.groovy.service;

import java.util.Arrays;

import com.mumu.game.groovy.IGroovyExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.util.GroovyScriptEngine;
import lombok.extern.slf4j.Slf4j;

/**
 * GroovyService
 * groovy 执行脚本服务
 * @author liuzhen
 * @version 1.0.0 2026/8/20 09:34
 */
@Slf4j
@Service
public class GroovyService {

  static final String GROOVY_SUFFIX = ".groovy";

  @Autowired(required = false)
  GroovyScriptEngine engine;

  /** 加载磁盘脚本文件并执行，本地脚本必须实现 IGroovyExecutor 接口 */
  public Object loadScriptAndExecute(String scriptName, String... args) {
    log.info(
        "Groovy.loadScriptAndExecute! scriptName: {}, args: {}", scriptName, Arrays.toString(args));
    if (engine == null) return "未开启Groovy功能";

    try {
      Class groovyClazz = engine.loadScriptByName(scriptName + GROOVY_SUFFIX);
      IGroovyExecutor executor = (IGroovyExecutor) groovyClazz.newInstance();
      return executor.execute(args);
    } catch (Exception e) {
      log.error(
          "Groovy.loadScriptAndExecute! scriptName: {}, args: {}",
          scriptName,
          Arrays.toString(args),
          e);
      return "执行失败：" + e.getMessage();
    }
  }

  /** 直接执行groovy脚本 */
  public Object evaluate(String scriptText, String... args) {
    log.info("Groovy.evaluate! scriptText: {}, args: {}", scriptText, Arrays.toString(args));
    try {
      return new GroovyShell(new Binding(args)).evaluate(scriptText);
    } catch (Exception e) {
      log.error("Groovy.evaluate! scriptText: {}, args: {}", scriptText, Arrays.toString(args), e);
      return "执行失败：" + e.getMessage();
    }
  }
}
