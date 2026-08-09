// package com.mumu.game.template.shop.core;
//
// import java.util.ArrayList;
// import java.util.Collections;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
//
// import com.mumu.game.business.shop.luban.ShopConfigManager;
// import com.mumu.game.business.shop.luban.dto.ConfigShopDTO;
// import com.mumu.game.core.autoinit.AutoInitEvent;
// import com.mumu.game.core.autoinit.AutoInitManager;
// import com.mumu.game.core.autoinit.enums.AutoInitModule;
// import com.mumu.game.core.utils.ModifierUtil;
// import com.mumu.game.core.utils.SpringContextUtils;
// import com.mumu.game.template.shop.core.anno.ShopType;
// import com.mumu.game.template.shop.core.template.AbstractShopTemp;
// import com.mumu.game.template.shop.core.template.DefaultShopTemp;
// import com.mumu.game.template.shop.core.template.ShopTemp;
// import org.springframework.stereotype.Component;
//
// import com.google.common.collect.ImmutableMap;
//
// /**
//  * ShopTemplateManager 商品模版管理器
//  *
//  * @author liuzhen
//  * @version 1.0.0 2024/11/19 20:40
//  */
// @Component
// public class ShopTemplateManager implements AutoInitEvent, AutoLubanEvent<ShopConfigLoader> {
//
//   private static final Map<Integer, Class<? extends AbstractShopTemp>> SHOP_TYPE_CLAZZ_MAP =
//       new HashMap<>();
//
//   /** 商城模版map key: 商品id; value: 商城模版 */
//   private static Map<Integer, ShopTemp> goodsIdTemplateMap = Collections.emptyMap();
//
//   /** 商城模版map key: 商品类型; value: 商城模版 */
//   private static Map<Integer, ShopTemp> goodsTypeTemplateMap = Collections.emptyMap();
//
//   /** 功能id 与 商城模版列表 映射 */
//   private static Map<Integer, List<ShopTemp>> functionIdDhopTemplatesMap =
//       Collections.emptyMap();
//
//   @Override
//   public void autoInit() {
//     findTypeTemplate();
//     initConfig();
//   }
//
//   @Override
//   public void autoLubanRefresh() {
//     initConfig();
//   }
//
//   /** 查找商品类型模板 */
//   @SuppressWarnings("unchecked")
//   private static void findTypeTemplate() {
//     for (Class<?> c : AutoInitManager.CLASSES) {
//       ShopType annotation = c.getAnnotation(ShopType.class);
//       if (annotation != null && ModifierUtil.isBelongTo(c, AbstractShopTemp.class)) {
//         for (int typeId : annotation.value()) {
//           SHOP_TYPE_CLAZZ_MAP.put(typeId, (Class<? extends AbstractShopTemp>) c);
//         }
//       }
//     }
//   }
//
//   /**
//    * 初始化配置
//    *
//    * @since 2024/12/11 20:15
//    */
//   private void initConfig() {
//     Map<Integer, ShopTemp> tmpGoodsIdTemplateMap = new HashMap<>();
//     Map<Integer, ShopTemp> tmpGoodsTypeTemplateMap = new HashMap<>();
//     Map<Integer, List<ShopTemp>> tmpFunctionIdDhopTemplatesMap = new HashMap<>();
//
//     Map<Integer, Integer> goodsTypeFuncIdMap = ShopConfigManager.getGoodsTypeFuncIdMap();
//     for (Map.Entry<Integer, Integer> entry : goodsTypeFuncIdMap.entrySet()) {
//       int goodsType = entry.getKey();
//       int functionId = entry.getValue();
//
//       AbstractShopTemp template = SpringContextUtils.getBean(getTemplateClazz(goodsType));
//       template.setGoodsType(goodsType);
//
//       tmpGoodsTypeTemplateMap.put(goodsType, template);
//       tmpFunctionIdDhopTemplatesMap
//           .computeIfAbsent(functionId, k -> new ArrayList<>())
//           .add(template);
//     }
//
//     ImmutableMap<Integer, ConfigShopDTO> goodsIdConfigShopMap = ShopConfigManager.getGoodsIdConfigShopMap();
//     for (Map.Entry<Integer, ConfigShopDTO> entry : goodsIdConfigShopMap.entrySet()) {
//       int goodsId = entry.getKey();
//       ConfigShopDTO configShop = entry.getValue();
//       tmpGoodsIdTemplateMap.put(goodsId, tmpGoodsTypeTemplateMap.get(configShop.getType()));
//     }
//
//     goodsIdTemplateMap = tmpGoodsIdTemplateMap;
//     goodsTypeTemplateMap = tmpGoodsTypeTemplateMap;
//     functionIdDhopTemplatesMap = tmpFunctionIdDhopTemplatesMap;
//   }
//
//   /** 获取clazz */
//   private Class<? extends AbstractShopTemp> getTemplateClazz(int goodsType) {
//     return SHOP_TYPE_CLAZZ_MAP.getOrDefault(goodsType, DefaultShopTemp.class);
//   }
//
//   @Override
//   public AutoInitModule getInitGroup() {
//     return AutoInitModule.COMMON;
//   }
//
//   /**
//    * 获取商品模版
//    *
//    * @param goodsId 商品id
//    * @return com.game.template.shop.core.template.ShopTemplate
//    * @since 2024/11/20 10:17
//    */
//   public static ShopTemp getShopTemplateByGoodsId(int goodsId) {
//     return goodsIdTemplateMap.get(goodsId);
//   }
//
//   /**
//    * 获取商品模版
//    *
//    * @param goodsType goodsType
//    * @return com.game.template.shop.core.template.ShopTemplate
//    * @since 2024/11/20 10:17
//    */
//   public static ShopTemp getShopTemplate(int goodsType) {
//     return goodsTypeTemplateMap.get(goodsType);
//   }
//
//   /**
//    * 获取商品模版列表
//    *
//    * @param functionId 功能id
//    * @return java.util.List<com.game.template.shop.core.template.ShopTemplate>
//    * @since 2024/11/19 20:42
//    */
//   public static List<ShopTemp> getShopTemplateList(int functionId) {
//     return functionIdDhopTemplatesMap.getOrDefault(functionId, Collections.emptyList());
//   }
// }
