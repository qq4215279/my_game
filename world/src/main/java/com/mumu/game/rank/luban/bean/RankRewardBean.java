package com.mumu.game.rank.luban.bean;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;
import com.mumu.game.proto.item.ItemBean;
import lombok.Data;


/**
 * RankRewardBean
 * 排行榜掉落排名奖励
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class RankRewardBean {
  /** 排名范围Min */
  private Integer min;
  /** 排名范围Max */
  private Integer max;
  /** 奖励列表 */
  private java.util.List<ItemBean> rewards = new java.util.ArrayList<>();
}