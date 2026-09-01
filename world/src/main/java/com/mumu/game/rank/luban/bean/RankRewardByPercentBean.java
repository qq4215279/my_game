package com.mumu.game.rank.luban.bean;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;
import lombok.Data;


/**
 * RankRewardByPercentBean
 * 排行榜百分比排名奖励
 * @author Auto-generated
 * @version 1.0.0 2024/9/9 10:00
 */
@ProtobufClass
@Data
public class RankRewardByPercentBean {
  /** 排名范围Min */
  private Integer min;
  /** 排名范围Max */
  private Integer max;
  /** 百分比 */
  private Integer percent;
}