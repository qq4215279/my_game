package com.mumu.game.proto.function;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import lombok.Data;


/**
 * ConfigFunctionInfoBean
 * 功能配置基本信息
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class ConfigFunctionInfoBean {
  /** 父功能id */
  private Integer parentId;
  /** 名称 */
  private String name;
  /** 描述 */
  private String desc;
  /** 是否关闭 */
  private Boolean close;
  /** 排序 */
  private Integer sort;
  /** 功能额外信息( type:value;type:value;  已有类型：type-0, value-活动预热时间(单位分钟)  */
  private String extraInfo;
}