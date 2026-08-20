package com.mumu.game.groovy.controller;

import java.util.List;

import com.mumu.game.groovy.service.GroovyService;
import com.mumu.game.http.HttpResult;
import com.mumu.game.utils.EnvUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import jakarta.annotation.Resource;

/**
 * GroovyController
 * groovy脚本执行
 * @author liuzhen
 * @version 1.0.0 2026/8/20 09:34
 */
@RestController
@RequestMapping("/groovy/")
public class GroovyController {

  @Resource
  GroovyService groovyService;

  /** 执行本地Groovy脚本 */
  @GetMapping("loadScript/{groovyFileName}")
  public HttpResult loadScript(
      @PathVariable("groovyFileName") String groovyFileName,
      @RequestParam(value = "args", required = false) List<String> args) {
    String[] params = args == null ? new String[0] : args.toArray(new String[0]);
    Object result = groovyService.loadScriptAndExecute(groovyFileName, params);
    return HttpResult.success("执行" + groovyFileName).add("result", result);
  }

  /** 直接执行groovy脚本 */
  @PostMapping(value = "evaluate", consumes = "text/plain")
  public HttpResult evaluate(
      @RequestParam(value = "args", required = false) List<String> args,
      @RequestBody String scriptText) {
    if (EnvUtil.isProd()) {
      return HttpResult.error("非开发环境不允许执行远程脚本！");
    }
    String[] params = args == null ? new String[0] : args.toArray(new String[0]);
    Object result = groovyService.evaluate(scriptText, params);
    return HttpResult.success("执行 evaluate").add("result", result);
  }
}
