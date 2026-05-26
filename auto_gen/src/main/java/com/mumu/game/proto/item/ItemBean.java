package com.mumu.game.proto.item;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;
import lombok.Data;

/**
 * ItemBean
 *
 * @author liuzhen
 * @version 1.0.0 2026/5/25 19:39
 */
@Data
@ProtobufClass
public class ItemBean {
    /** 道具id */
    private Integer id;
    /** 道具数量 */
    private Long num;
    /** 时间【秒级】 */
    private Long time;
    /** 过期时间 永久:-1 */
    private Long expireTime;
    /** 是否有红点 */
    private Boolean hasPoint;
}
