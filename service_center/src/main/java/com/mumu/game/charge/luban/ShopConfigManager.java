package com.mumu.game.charge.luban;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.mumu.game.charge.conf.ConfigPayID;
import com.mumu.game.charge.conf.ConfigShop;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;

import lombok.Getter;

/**
 * ShopConfigManager 商品静态缓存
 *
 * @author liuzhen
 * @version 1.0.0 2024/9/25 13:41
 */
@Component
public class ShopConfigManager {

  /** goodsId 与 商品 映射 */
  @Getter private static ImmutableMap<Integer, ConfigShop> goodsIdConfigShopMap = ImmutableMap.of();

  /** 商品类型 与 商品列表 映射 */
  private static Map<Integer, List<ConfigShop>> goodsTypeConfigShopsMap =
      ImmutableMap.of();

  /** goodsId 与 ConfigPayID 映射 */
  private static ImmutableMap<Integer, ConfigPayID> goodsIdConfigPayIdMap = ImmutableMap.of();

  /** productId 与 ConfigPayID 映射 */
  private static Map<String, ConfigPayID> productIdConfigPayIdMap = Collections.emptyMap();

  public void autoLoad() {

    Collection<ConfigShop> configShopList = new ArrayList<>();

    goodsTypeConfigShopsMap =
        configShopList.stream()
            .sorted(Comparator.comparingInt(ConfigShop::getSort))
            .collect(Collectors.groupingBy(ConfigShop::getType));
    // goodsTypeConfigShopsMap =
    //     ImmutableUtil.list2ImmMapWithList(configShopList, ConfigShop::getType);

    Collection<ConfigPayID> configPayIDList = new ArrayList<>();

    productIdConfigPayIdMap = new HashMap<>();
    for (ConfigPayID configPayID : configPayIDList) {
      productIdConfigPayIdMap.put(configPayID.getHuawei(), configPayID);
      productIdConfigPayIdMap.put(configPayID.getIos(), configPayID);
      productIdConfigPayIdMap.put(configPayID.getGoogleplay(), configPayID);
      productIdConfigPayIdMap.put(configPayID.getProd(), configPayID);
    }
  }

  /**
   * 获取商品信息
   *
   * @param goodsId 商品id
   * @return com.game.luban.hall.shop.ConfigShop
   * @since 2024/9/25 15:08
   */
  public static ConfigShop getConfigShop(int goodsId) {
    return goodsIdConfigShopMap.get(goodsId);
  }

  /**
   * getConfigPayID
   *
   * @param goodsId 商品id
   * @return com.game.luban.hall.shop.ConfigPayID
   * @since 2024/10/10 11:59
   */
  public static ConfigPayID getConfigPayID(int goodsId) {
    return goodsIdConfigPayIdMap.get(goodsId);
  }

  /**
   * getConfigPayID
   * @param productId productId
   * @return com.game.luban.hall.shop.ConfigPayID
   * @since 2025/1/15 15:07
   */
  public static ConfigPayID getConfigPayIDByProductId(String productId) {
    return productIdConfigPayIdMap.get(productId);
  }
}
