package com.mumu.framework.business.language;

import lombok.Data;

/**
 * 国际化语言配置表
 *
 * @author Commuication Auto Maker
 *     转化的json字符串{\"type\":\"object\",\"displayType\":\"row\",\"showDescIcon\":\"true\",\"properties\":{\"data_id\":{\"title\":\"主键\",\"type\":\"string\",\"fkey\":\"data_id\",\"tooltip\":\"国际化语言key\",\"labelWidth\":200,\"props\":{}},\"ar\":{\"title\":\"阿拉伯语\",\"type\":\"string\",\"fkey\":\"input_xdlHWS\",\"labelWidth\":200,\"props\":{},\"isCnPattern\":true,\"__allowBlank__\":true,\"__allowAroundBlank__\":true},\"zhcn\":{\"title\":\"中文简体\",\"type\":\"string\",\"fkey\":\"input_faZEFr\",\"labelWidth\":200,\"props\":{},\"isCnPattern\":true,\"__allowBlank__\":true,\"__allowAroundBlank__\":true},\"eng\":{\"title\":\"英文\",\"type\":\"string\",\"fkey\":\"input_Tna3P7\",\"labelWidth\":200,\"props\":{}},\"jap\":{\"title\":\"日语\",\"type\":\"string\",\"fkey\":\"input_9S-m98\",\"labelWidth\":200,\"props\":{},\"isCnPattern\":true,\"__allowBlank__\":true,\"__allowAroundBlank__\":true},\"desc\":{\"title\":\"描述\",\"type\":\"string\",\"fkey\":\"input_fbx-Br\",\"labelWidth\":200,\"props\":{},\"isCnPattern\":true,\"__allowBlank__\":true,\"__allowAroundBlank__\":true,\"__allowIgnore__\":true}}}
 */
@Data
public class ConfigMultiLanguage {


  /** 主键 */
  private String data_id;

  /** 阿拉伯语 */
  private String ar;

  /** 中文简体 */
  private String zhcn;

  /** 英文 */
  private String eng;

  /** 日语 */
  private String jap;

  /** 描述 */
  private String desc;

  @Override
  public String toString() {
    return "ConfigMultiLanguage{"
        + "data_id="
        + data_id
        + ",ar="
        + ar
        + ",zhcn="
        + zhcn
        + ",eng="
        + eng
        + ",jap="
        + jap
        + ",desc="
        + desc
        + "}";
  }
}
