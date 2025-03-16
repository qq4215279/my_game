package com.mumu.framework.business.language.enums;

import com.mumu.framework.business.language.ConfigMultiLanguage;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * LanguageEnum
 * 国际化枚举
 * @author liuzhen
 * @version 1.0.0 2024/12/31 14:38
 */
public enum LanguageEnum {
  /** 中文简体 */
  ZHCN("zhcn") {
    @Override
    public String getContent(String key) {
      // TODO
      ConfigMultiLanguage configMultiLanguage = new ConfigMultiLanguage();
      if (configMultiLanguage == null) {
        return "";
      }
      return configMultiLanguage.getZhcn();
    }
  },
  /** 阿拉伯语 */
  AR("ar") {
    @Override
    public String getContent(String key) {
      // TODO
      ConfigMultiLanguage configMultiLanguage = new ConfigMultiLanguage();
      if (configMultiLanguage == null) {
        return "";
      }
      return configMultiLanguage.getAr();
    }
  },
  /** 英文 */
  ENG("eng") {
    @Override
    public String getContent(String key) {
      // TODO
      ConfigMultiLanguage configMultiLanguage = new ConfigMultiLanguage();
      if (configMultiLanguage == null) {
        return "";
      }
      return configMultiLanguage.getEng();
    }
  },
  /** 日语 */
  JAP("jap") {
    @Override
    public String getContent(String key) {
      // TODO
      ConfigMultiLanguage configMultiLanguage = new ConfigMultiLanguage();
      if (configMultiLanguage == null) {
        return "";
      }
      return configMultiLanguage.getJap();
    }
  },

  ;

  /** 语言编码 */
  @Getter
  private final String languageCode;

  LanguageEnum(String languageCode) {
    this.languageCode = languageCode;
  }

  public abstract String getContent(String key);

  /** 语言类型map */
  private static final Map<String, LanguageEnum> languageCodeMap = new HashMap<>();
  static {
    for (LanguageEnum nEnum : values()) {
      languageCodeMap.put(nEnum.languageCode, nEnum);
    }
  }

  /**
   * 获取国际化枚举
   * @param languageCode languageCode
   * @return com.game.business.language.enums.LanguageEnum
   * @date 2024/12/31 14:55
   */
  public static LanguageEnum getLanguageEnum(String languageCode) {
    return languageCodeMap.getOrDefault(languageCode, ZHCN);
  }

  /**
   * 获取国际化内容
   * @param playerId 玩家id
   * @param key 国际化key
   * @return java.lang.String
   * @date 2024/12/31 14:59
   */
  public static String getContent(long playerId, String key) {
    // TODO
    // Player player = PlayerManager.self().getPlayer(playerId);
    // String languageCode = player.getLanguageCode();
    String languageCode = "zhcn";
    LanguageEnum languageEnum = getLanguageEnum(languageCode);
    return languageEnum.getContent(key);
  }

}
