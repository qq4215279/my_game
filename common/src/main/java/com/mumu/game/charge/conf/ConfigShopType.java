package com.mumu.game.charge.conf;


import lombok.Data;

/**
 * 商品类型表
 *
 * @author Commuication Auto Maker
 *     转化的json字符串{\"type\":\"object\",\"displayType\":\"row\",\"showDescIcon\":\"true\",\"properties\":{\"data_id\":{\"title\":\"主键\",\"type\":\"string\",\"fkey\":\"data_id\",\"tooltip\":\"商品类型\",\"labelWidth\":200,\"props\":{}},\"desc\":{\"title\":\"商品类型名称\",\"type\":\"string\",\"format\":\"textarea\",\"fkey\":\"textarea_sd-GvN\",\"labelWidth\":200,\"props\":{}}}}
 */
@Data
public class ConfigShopType {

  /** 主键 */
  private String data_id;

  /** 商品类型名称 */
  private String desc;

  public String getData_id() {
    return data_id;
  }

}
