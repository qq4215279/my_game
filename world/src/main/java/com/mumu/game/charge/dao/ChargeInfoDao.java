package com.mumu.game.charge.dao;

import java.util.Date;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.mumu.game.charge.entity.ChargeInfo;

/**
 * ChargeInfoDao
 * @author liuzhen
 * @version 1.0.0 2024/11/26 18:01
 */
@Mapper
public interface ChargeInfoDao {

  /**
   * getChargeInfo
   * @param orderId orderId
   * @return com.game.charge.entity.ChargeInfo
   * @since 2024/11/26 18:15
   */
  @Select("SELECT * FROM `charge_info` WHERE order_id = #{orderId};")
  @Results({
      @Result(property = "orderId", column = "order_Id"),
      @Result(property = "playerId", column = "player_id"),
      @Result(property = "goodsId", column = "goods_id"),
      @Result(property = "num", column = "num"),
      @Result(property = "payChannel", column = "pay_channel"),
      @Result(property = "payType", column = "pay_type"),
      @Result(property = "state", column = "state"),
      @Result(property = "productId", column = "product_id"),
      @Result(property = "channelOrderId", column = "channel_order_id"),
      @Result(property = "price", column = "price"),
      @Result(property = "payTime", column = "pay_time"),
      @Result(property = "createTime", column = "create_time"),
      @Result(property = "payInfo", column = "pay_info"),
      @Result(property = "extraInfo", column = "extra_info"),
      @Result(property = "extraGoodsId", column = "extra_goods_id"),
  })
  ChargeInfo getChargeInfo(String orderId);

  /**
   * getChargeInfoList
   * @param playerId playerId
   * @return java.util.List<com.game.charge.entity.ChargeInfo>
   * @since 2024/11/26 18:14
   */
  @Select("SELECT * FROM `charge_info` WHERE player_id = #{playerId};")
  @Results({
      @Result(property = "orderId", column = "order_Id"),
      @Result(property = "playerId", column = "player_id"),
      @Result(property = "goodsId", column = "goods_id"),
      @Result(property = "num", column = "num"),
      @Result(property = "payChannel", column = "pay_channel"),
      @Result(property = "payType", column = "pay_type"),
      @Result(property = "state", column = "state"),
      @Result(property = "productId", column = "product_id"),
      @Result(property = "channelOrderId", column = "channel_order_id"),
      @Result(property = "price", column = "price"),
      @Result(property = "payTime", column = "pay_time"),
      @Result(property = "createTime", column = "create_time"),
      @Result(property = "payInfo", column = "pay_info"),
      @Result(property = "extraInfo", column = "extra_info"),
      @Result(property = "extraGoodsId", column = "extra_goods_id"),
  })
  List<ChargeInfo> getChargeInfoList(long playerId);

  @Select("SELECT * FROM `charge_info` WHERE player_id = #{playerId} and product_id = #{productId} and channel_order_id = #{channelOrderId};")
  @Results({
      @Result(property = "orderId", column = "order_Id"),
      @Result(property = "playerId", column = "player_id"),
      @Result(property = "goodsId", column = "goods_id"),
      @Result(property = "num", column = "num"),
      @Result(property = "payChannel", column = "pay_channel"),
      @Result(property = "payType", column = "pay_type"),
      @Result(property = "state", column = "state"),
      @Result(property = "productId", column = "product_id"),
      @Result(property = "channelOrderId", column = "channel_order_id"),
      @Result(property = "price", column = "price"),
      @Result(property = "payTime", column = "pay_time"),
      @Result(property = "createTime", column = "create_time"),
      @Result(property = "payInfo", column = "pay_info"),
      @Result(property = "extraInfo", column = "extra_info"),
      @Result(property = "extraGoodsId", column = "extra_goods_id"),
  })
  ChargeInfo getChargeInfoByChannelOrderId(long playerId, String productId, String channelOrderId);

  /**
   * 新增订单
   * @param orderId orderId
   * @param playerId playerId
   * @param goodsId goodsId
   * @param payChannel payChannel
   * @param payType payType
   * @param state state
   * @param productId productId
   * @param price price
   * @param createTime createTime
   * @param extraInfo extraInfo
   * @param extraGoodsId extraGoodsId
   * @since 2024/11/27 11:48
   */
  @Insert("INSERT INTO charge_info VALUES(#{orderId}, #{playerId}, #{goodsId}, #{num}, #{payChannel}, #{payType}, #{state}, #{productId}, #{price}, null, #{createTime}, '', #{extraInfo}, #{extraGoodsId}, '');")
  void insertChargeInfo(String orderId, long playerId, int goodsId, int num, String payChannel,
      String payType, int state, String productId, int price, Date createTime
      , String extraInfo, int extraGoodsId);

  /**
   * 更新状态
   * @param orderId orderId
   * @param state state
   * @since 2024/11/27 15:37
   */
  @Update("UPDATE charge_info SET state = #{state}, pay_time = #{date} WHERE order_id = #{orderId};")
  void updateState(String orderId, int state, Date date);

  /**
   * 更新payInfo
   * @param orderId orderId
   * @param payInfo payInfo
   * @since 2024/11/28 16:25
   */
  @Update("UPDATE charge_info SET pay_info = #{payInfo} WHERE order_id = #{orderId};")
  void updatePayInfo(String orderId, String payInfo);

}
