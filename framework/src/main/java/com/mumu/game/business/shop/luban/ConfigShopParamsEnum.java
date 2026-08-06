package com.mumu.game.business.shop.luban;

import com.mumu.game.core.utils.CovertUtil;

import java.util.Optional;


/** 商城鲁班KY常量表 @Date: 2025/7/11 上午11:51  */
public enum ConfigShopParamsEnum  {
  /** 高额金币回购礼包每日触发次数 */
  GOLD_RETURN_DAILY_LIMIT(1),
  /** 高额金币回购礼包触发的延迟时间 ms */
  GOLD_RETURN_DELAY_TIME(1800000),
  /** 高额金币回购礼包客户端弹出的延迟时间 ms（1-baloot 2-bet 3-fruit） */
  GOLD_RETURN_POP_DELAY_TIME(3000),
  /** 高额金币回购礼包触发的比例 */
  GOLD_RETURN_LOSS_TRIGGER_RATE(50),
  /** 高额金币回购礼包的返还消耗比 k-损失金币 v-对应钻石消耗的比值（1-baloot 2-bet 3-fruit） */
  GOLD_RETURN_LOSS_COST_RATE("10000,200;100000,250") {
    // @Override
    public Object parse(String val) {
      return CovertUtil.stringToTreeMap(val, Long::parseLong, Integer::parseInt);
    }
  },
  ;

  /** 默认值 */
  private final String value;

  ConfigShopParamsEnum(Object value) {
    this.value = String.valueOf(value);
  }

  /*@Override
  public String getKey() {
    return name();
  }

  @Override
  public String getDefaultValue() {
    return value;
  }

  @Override
  public String getStringValue(String key, String defaultValue) {
    return Optional.ofNullable(getLubanLoader().getConfigShopParams(key))
        .map(ConfigShopParams::getValue)
        .orElse(defaultValue);
  }*/
}
