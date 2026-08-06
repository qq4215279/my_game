package com.mumu.game.charge.dao;

import java.util.List;

import com.mumu.game.account.entity.XsollaChargeInfo;
import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * XsollaChargeInfoDao
 * 第三方支付订单信息Dao
 * @author liuzhen
 * @version 1.0.0 2025/6/11 16:56
 */
@Mapper
public interface XsollaChargeInfoDao extends BaseMapper<XsollaChargeInfo> {

  /**
   * 获取第三方支付订单信息
   * @param channelOrderId channelOrderId 
   * @return com.game.charge.entity.XsollaChargeInfo
   * @since 2025/6/11 16:58
   */
  default XsollaChargeInfo getXsollaChargeInfo(int channelOrderId) {
    return selectById(channelOrderId);
  }

  /**
   * 获取订单列表
   * @param playerId 玩家id
   * @param state 订单状态 0: 未全部发货; 1: 全部已发货
   * @return java.util.List<com.game.charge.entity.XsollaChargeInfo>
   * @since 2025/6/11 17:01
   */
  default List<XsollaChargeInfo> getChargeInfoListByState(long playerId, int state) {
    return selectList(
        new LambdaQueryWrapper<XsollaChargeInfo>().eq(XsollaChargeInfo::getPlayerId, playerId).eq(
            XsollaChargeInfo::getState, state));
  }

  /**
   * 获取订单列表
   * @param state 订单状态 0: 未全部发货; 1: 全部已发货
   * @return java.util.List<com.game.charge.entity.XsollaChargeInfo>
   * @since 2025/6/11 17:01
   */
  default List<XsollaChargeInfo> getChargeInfoListByState(int state) {
    return selectList(
        new LambdaQueryWrapper<XsollaChargeInfo>().eq(XsollaChargeInfo::getState, state));
  }

  /**
   * 插入
   * @param xsollaChargeInfo xsollaChargeInfo
   * @since 2025/6/11 16:58
   */
  default void insertXsollaChargeInfo(XsollaChargeInfo xsollaChargeInfo) {
    insert(xsollaChargeInfo);
  }

}
