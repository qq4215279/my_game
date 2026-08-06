package com.mumu.game.business.shop.luban;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Pair;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.mumu.game.business.shop.luban.dto.ConfigShopDTO;
import com.mumu.game.charge.conf.ConfigPayID;
import com.mumu.game.charge.conf.ConfigShopTab;
import com.mumu.game.constants.Symbol;
import com.mumu.game.core.utils.CovertUtil;
import com.mumu.game.core.utils.ImmutableUtil;
import com.mumu.game.core.utils.SpringContextUtils;
import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * ShopConfigManager 商品静态缓存
 *
 * @author liuzhen
 * @version 1.0.0 2024/9/25 13:41
 */
@Component
public class ShopConfigManager  {

  public static ShopConfigManager self() {
    return SpringContextUtils.getBean(ShopConfigManager.class);
  }

  /** 功能id 与 商品类型列表 映射 */
  private static Map<Integer, List<Integer>> funcIdGoodsTypesMap = Collections.emptyMap();

  /** 商品类型 与 功能id 映射 */
  @Getter private static Map<Integer, Integer> goodsTypeFuncIdMap = Collections.emptyMap();

  /** goodsId 与 商品 映射 */
  @Getter
  private static ImmutableMap<Integer, ConfigShopDTO> goodsIdConfigShopMap = ImmutableMap.of();

  /** 商品类型 与 商品列表 映射 */
  private static Map<Integer, List<ConfigShopDTO>> goodsTypeConfigShopsMap = ImmutableMap.of();

  /** k1-道具id k2-获得数量 val-goodsId映射 */
  private static Map<Integer, TreeMap<Long, Integer>> itemIdPopGoodsMap = Collections.emptyMap();

  /** goodsId 与 ConfigPayID 映射 */
  private static ImmutableMap<Integer, ConfigPayID> goodsIdConfigPayIdMap = ImmutableMap.of();

  /** productId 与 ConfigPayID 映射 */
  private static Map<String, ConfigPayID> productIdConfigPayIdMap = Collections.emptyMap();

  /** 弹脸商品id 与 弹脸信息 映射 */
  // private static volatile ImmutableMap<Integer, ConfigPopFaceGoods> goodsIdPopFaceGoodsMap;

  /** 触发弹脸任务id 与 弹脸信息 映射 */
  // private static volatile ImmutableMap<Integer, ConfigPopFaceGoods> taskIdPopFaceGoodsMap;

  public void autoLoad() {
    Collection<ConfigShopTab> configShopTabList = new ArrayList<>();

    funcIdGoodsTypesMap =
        configShopTabList.stream()
            .collect(
                Collectors.toMap(
                    conf -> Integer.parseInt(conf.getData_id()),
                    conf -> CovertUtil.stringToIntList(conf.getGoodsTypes(), Symbol.COMMA)));

    goodsTypeFuncIdMap =
        configShopTabList.stream()
            .flatMap(
                o ->
                    Arrays.stream(o.getGoodsTypes().split(Symbol.COMMA))
                        .map(Integer::parseInt)
                        .map(goodsType -> new Pair<>(Integer.parseInt(o.getData_id()), goodsType)))
            .collect(Collectors.toMap(Pair::getValue, Pair::getKey, (o1, o2) -> o2));

    Collection<ConfigShopDTO> configShopList = new ArrayList<>();
    goodsIdConfigShopMap = ImmutableUtil.list2ImmMap(configShopList, ConfigShopDTO::getGoodsId);

    goodsTypeConfigShopsMap =
        configShopList.stream()
            .sorted(Comparator.comparingInt(ConfigShopDTO::getSort))
            .collect(Collectors.groupingBy(ConfigShopDTO::getType));
    // goodsTypeConfigShopsMap =
    //     ImmutableUtil.list2ImmMapWithList(configShopList, ConfigShop::getType);

    itemIdPopGoodsMap = null;
        /*loader.getConfigPopGoodsMap().values().stream()
            .collect(
                Collectors.groupingBy(
                    c -> Integer.parseInt(c.getItemId()),
                    Collectors.toMap(
                        c -> (long) c.getCount(),
                        c -> Integer.parseInt(c.getGoodsId()),
                        (c1, c2) -> c2,
                        TreeMap::new)));*/

    Collection<ConfigPayID> configPayIDList = new ArrayList<>();;
    goodsIdConfigPayIdMap =
        ImmutableUtil.list2ImmMap(configPayIDList, ConfigPayID::getGoodsId);

    productIdConfigPayIdMap = new HashMap<>();
    for (ConfigPayID configPayID : configPayIDList) {
      productIdConfigPayIdMap.put(configPayID.getHuawei(), configPayID);
      productIdConfigPayIdMap.put(configPayID.getIos(), configPayID);
      productIdConfigPayIdMap.put(configPayID.getGoogleplay(), configPayID);
      productIdConfigPayIdMap.put(configPayID.getProd(), configPayID);
    }

    // 弹脸
    // goodsIdPopFaceGoodsMap = null;
        // ImmutableUtil.list2ImmMap(configPopFaceGoods, o -> Integer.parseInt(o.getData_id()));
    // taskIdPopFaceGoodsMap = null;
        /*ImmutableUtil.list2MultiKeyImmMap(
            configPopFaceGoods,
            o -> CovertUtil.stringToIntList(o.getTriggerTaskIds(), Symbol.COMMA));*/
  }

  /**
   * 获取商品类型列表
   *
   * @param functionId 功能id
   * @return java.util.List<java.lang.Integer>
   * @since 2024/9/25 15:06
   */
  public static List<Integer> getGoodsTypeListByFuncId(int functionId) {
    return funcIdGoodsTypesMap.getOrDefault(functionId, Collections.emptyList());
  }

  /** 获取第一个商品类型 */
  public static Integer getFirstGoodsTypeByFuncId(int functionId) {
    Integer first = CollUtil.getFirst(getGoodsTypeListByFuncId(functionId));
    return first == null ? 0 : first;
  }

  /** 获取功能下第一个商品类型的第一个商品 */
  public static ConfigShopDTO getFirstShopByFuncId(int functionId) {
    return getFirstShopByType(getFirstGoodsTypeByFuncId(functionId));
  }

  /**
   * @param goodsType 商品类型
   * @return int
   * @since 2024/9/25 15:07
   */
  public static int getFunctionId(int goodsType) {
    return goodsTypeFuncIdMap.getOrDefault(goodsType, -1);
  }

  /**
   * 获取商品信息
   *
   * @param goodsId 商品id
   * @return com.game.luban.hall.shop.ConfigShop
   * @since 2024/9/25 15:08
   */
  public static ConfigShopDTO getConfigShop(int goodsId) {
    return goodsIdConfigShopMap.get(goodsId);
  }

  /**
   * 获取商品信息列表
   *
   * @param goodsType 商品类型
   * @return java.util.List<com.game.luban.hall.shop.ConfigShop>
   * @since 2024/9/25 15:08
   */
  public static List<ConfigShopDTO> getConfigShopList(int goodsType) {
    return goodsTypeConfigShopsMap.getOrDefault(goodsType, Collections.emptyList());
  }

  /** 获取商品类型下的第一个商品 */
  public static ConfigShopDTO getFirstShopByType(int goodsType) {
    return CollUtil.getFirst(getConfigShopList(goodsType));
  }

  /**
   * 获取指定道具可弹窗的商品列表
   *
   * @param itemId 道具id
   * @since 2024/10/9 20:57
   */
  public static TreeMap<Long, Integer> getPopGoodsMap(int itemId) {
    return itemIdPopGoodsMap.get(itemId);
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
   *
   * @param productId productId
   * @return com.game.luban.hall.shop.ConfigPayID
   * @since 2025/1/15 15:07
   */
  public static ConfigPayID getConfigPayIDByProductId(String productId) {
    return productIdConfigPayIdMap.get(productId);
  }

/*  *//**
   * 获取弹脸信息
   *
   * @param goodsId goodsId
   * @return com.game.luban.hall.shop.ConfigPopFaceGoods
   * @since 2025/7/8 17:10
   *//*
  public static ConfigPopFaceGoods getConfigPopFaceGoods(int goodsId) {
    return goodsIdPopFaceGoodsMap.get(goodsId);
  }

  *//**
   * 获取弹脸信息By任务id
   *
   * @param taskId taskId
   * @return com.game.luban.hall.shop.ConfigPopFaceGoods
   * @since 2025/7/8 17:10
   *//*
  public static ConfigPopFaceGoods getConfigPopFaceGoodsByTaskId(int taskId) {
    return taskIdPopFaceGoodsMap.get(taskId);
  }*/
}
