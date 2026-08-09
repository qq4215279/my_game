package com.mumu.game.proto.shop.goods;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

import com.mumu.game.proto.shop.GoodsBean;
import lombok.Data;


/**
 * OnWCGoldReturnMessage
 * 推送高额金币回购礼包信息
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class OnWCGoldReturnMessage {
  /** 商品信息 */
  private GoodsBean goodsBean;
  /** 延迟时间 */
  private Integer delayTime;
}