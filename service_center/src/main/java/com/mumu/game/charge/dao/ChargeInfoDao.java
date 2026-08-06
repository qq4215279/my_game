package com.mumu.game.charge.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.mumu.game.charge.entity.ChargeInfo;

import java.util.Collections;
import java.util.List;

/**
 * ChargeInfoDao
 *
 * @author liuzhen
 * @version 1.0.0 2026/8/2 13:51
 */
public interface ChargeInfoDao extends MongoRepository<ChargeInfo, String> {

    default ChargeInfo getChargeInfo(String chargeId) {
        return this.findById(chargeId).orElse(null);
    }


    /**
     * getChargeInfoList
     * @param playerId playerId
     * @return java.util.List<com.game.entity.charge.ChargeInfo>
     * @since 2024/11/26 18:14
     */
    default List<ChargeInfo> getChargeInfoList(long playerId) {
        // return selectList(new LambdaQueryWrapper<ChargeInfo>().eq(ChargeInfo::getPlayerId, playerId));
        return Collections.emptyList();
    }

    /**
     * 获取最新订单信息
     * @param playerId playerId
     * @param productId productId
     * @return com.game.entity.charge.ChargeInfo
     * @since 2025/1/15 14:36
     */
    default ChargeInfo getLatestChargeInfo(long playerId, String productId) {
        /*return selectOne(
                new LambdaQueryWrapper<ChargeInfo>().eq(ChargeInfo::getPlayerId, playerId)
                        .eq(ChargeInfo::getProductId, productId).orderByDesc(ChargeInfo::getCreateTime) .last("LIMIT 1"));*/
        return null;
    }

    /**
     * 获取订单信息By渠道商订单号
     * @param playerId playerId
     * @param productId productId
     * @param channelOrderId 渠道商订单号
     * @return com.game.entity.charge.ChargeInfo
     * @since 2025/1/20 11:21
     */
    default ChargeInfo getChargeInfoByChannelOrderId(long playerId, String productId, String channelOrderId) {
        // return selectOne(
        //         new LambdaQueryWrapper<ChargeInfo>().eq(ChargeInfo::getPlayerId, playerId)
        //                 .eq(ChargeInfo::getProductId, productId)
        //                 .eq(ChargeInfo::getChannelOrderId, channelOrderId));
        return null;
    }

    /**
     * 获取最新ChannelOrderId
     * @param playerId playerId
     * @param payChannel payChannel
     * @return com.game.entity.charge.ChargeInfo
     * @since 2025/2/28 16:09
     */
    default String getLatestChannelOrderIdByPayChannel(long playerId, String payChannel) {
       /* ChargeInfo chargeInfo = selectOne(
                new LambdaQueryWrapper<ChargeInfo>().eq(ChargeInfo::getPlayerId, playerId)
                        .eq(ChargeInfo::getPayChannel, payChannel)
                        .ne(ChargeInfo::getChannelOrderId, "").orderByDesc(ChargeInfo::getCreateTime)
                        .last("LIMIT 1"));

        if (chargeInfo == null) {
            return "";
        }
        return chargeInfo.getChannelOrderId();*/
        return "";
    }


    /**
     * 根据productId分组，查询最近一条订单信息
     * @param playerId playerId
     * @param state state
     * @return java.util.List<com.game.entity.charge.ChargeInfo>
     * @since 2025/3/6 14:44
     */
    @Select("SELECT ci.* FROM charge_info ci INNER JOIN(SELECT product_id, order_Id, MAX(create_time) AS max_create_time FROM charge_info WHERE player_id = #{playerId} and state = #{state} GROUP BY product_id) grouped_ci ON ci.product_id = grouped_ci.product_id AND ci.create_time = grouped_ci.max_create_time;")
    @Results({
            @Result(property = "orderId", column = "order_Id"),
            @Result(property = "playerId", column = "player_id"),
            @Result(property = "goodsId", column = "goods_id"),
            @Result(property = "num", column = "num"),
            @Result(property = "payChannel", column = "pay_channel"),
            @Result(property = "payType", column = "pay_type"),
            @Result(property = "productId", column = "product_id"),
            @Result(property = "payTime", column = "pay_time"),
            @Result(property = "createTime", column = "create_time"),
            @Result(property = "payInfo", column = "pay_info"),
            @Result(property = "extraInfo", column = "extra_info"),
            @Result(property = "extraGoodsId", column = "extra_goods_id"),
    })
    List<ChargeInfo> getChargeInfoListByApple(long playerId, int state);
}
