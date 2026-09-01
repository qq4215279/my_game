package com.mumu.game.rank.luban;

import java.util.Optional;

import com.game.framework.core.autoluban.AutoLubanParam;
import com.game.luban.activity.component.ComponentConfigLoader;
import com.game.luban.activity.component.ConfigActivityParams;

/**
 * ConfigActivityParamsEnum
 * @author liuzhen
 * @version 1.0.0 2025/3/17 15:04
 */
public enum ConfigActivityParamsEnum implements AutoLubanParam<ComponentConfigLoader> {
  ;

  /** 默认值 */
  private final String value;

  ConfigActivityParamsEnum(Object value) {
    this.value = String.valueOf(value);
  }

  @Override
  public String getKey() {
    return name();
  }

  @Override
  public String getDefaultValue() {
    return value;
  }

  @Override
  public String getStringValue(String key, String defaultValue) {
    return Optional.ofNullable(getLubanLoader().getConfigActivityParams(key))
        .map(ConfigActivityParams::getValue)
        .orElse(defaultValue);
  }
}
