package com.mumu.game.business.shop.luban.dto;

import java.util.List;

import com.mumu.game.business.shop.operator.PlayerBuyGoodsDOOperator;
import com.mumu.game.charge.conf.ConfigShop;
import com.mumu.game.core.drop.core.Drop;
import com.mumu.game.proto.item.ItemBean;
import com.mumu.game.proto.shop.CommonGoodsBean;
import com.mumu.game.proto.shop.GoodsBean;
import com.mumu.game.template.func.enums.ResetEnum;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

/** 商品配置 @Date: 2025/7/9 下午2:45  */
@Getter
@ToString
@EqualsAndHashCode(callSuper = false)
public class ConfigShopDTO {
  /** 商品id */
  private final int goodsId;

  /** 商品类型 */
  private final int type;

  /** 商品名称 */
  private final String name;

  /** 商品描述 */
  private final String desc;

  /** 中文对照 */
  private final String remark;

  /** 价格 */
  private final String price;

  /** 价格bean对象 */
  private final ItemBean priceBean;

  /** 原价 */
  private final String originPrice;

  /** 原价bean对象 */
  private final ItemBean originPriceBean;

  /** 折扣 */
  private final int discount;

  /** 获得道具 */
  private final String rewards;

  /** 获得道具Bean */
  private final List<ItemBean> rewardsBeans;

  /** 额外获得 */
  private final String extraRewards;

  /** 额外获得Bean */
  private final List<ItemBean> extraRewardsBeans;

  /** 原奖励 */
  private final String originRewards;

  /** 原奖励Bean */
  private final List<ItemBean> originRewardsBeans;

  /** 限购类型 */
  private final ResetEnum resetType;

  /** 限购数量 */
  private final int limitCount;

  /** 商品排序 */
  private final int sort;

  /** 商品图标 */
  private final String icon;

  /** 角标 */
  private final String tag;

  /** 礼包标题 */
  private final String tile;

  /** 平台id */
  private final String platform;

  /** 限定渠道 */
  private final String channel;

  /** 额外参数 */
  private final String param;

  /** 上架时间 */
  private final long startBuyTime;

  /** 结束时间 */
  private final long endBuyTime;

  public ConfigShopDTO(ConfigShop conf) {
    this.goodsId = conf.getGoodsId();
    this.type = conf.getType();
    this.name = conf.getName();
    this.desc = conf.getDesc();
    this.remark = conf.getInput_hBVlcK();
    // 价格相关
    this.price = StringUtils.isEmpty(conf.getPrice()) ? "" : conf.getPrice();
    this.priceBean = Drop.of(price).buildItemBean0();

    this.originPrice = conf.getOriginPrice();
    this.originPriceBean = Drop.of(originPrice).buildItemBean0();

    // 奖励相关
    this.rewards = conf.getRewards();
    this.rewardsBeans = Drop.of(rewards).buildItemBeans();

    this.extraRewards = conf.getExtraRewards();
    this.extraRewardsBeans = Drop.of(extraRewards).buildItemBeans();

    this.originRewards = conf.getOriginRewards();
    this.originRewardsBeans = Drop.of(originRewards).buildItemBeans();

    this.discount = conf.getDiscount();
    this.resetType = ResetEnum.get(Integer.parseInt(conf.getLimitType()));
    this.limitCount = conf.getLimitCount();
    this.sort = conf.getSort();
    this.icon = conf.getIcon();
    this.tag = conf.getTag();
    this.tile = conf.getTile();
    this.platform = conf.getPlatform();
    this.channel = conf.getChannel();
    this.param = conf.getParam();
    this.startBuyTime = conf.getStartBuyTime();
    this.endBuyTime = conf.getEndBuyTime();
  }

  /** 是否是免费礼包 */
  public boolean isFree() {
    // TODO
    // return RewardEnum.FREE.hasDropItem(price);
    return false;

  }

  /** 是否是付费礼包 */
  public boolean isCurrency() {
    // TODO
    return true;
  }

  /** 是否是限购商品 */
  public boolean isLimit() {
    return getLimitCount() != -1;
  }

  /** 库存不足指定数量 */
  public boolean notStock(long playerId) {
    return notStock(playerId, 1);
  }

  /** 检查玩家当前商品的库存不足 */
  public boolean notStock(long playerId, int num) {
    if (!isLimit()) return false;
    return num + PlayerBuyGoodsDOOperator.self().getCount(playerId, getGoodsId()) > getLimitCount();
  }

  /** DTO转GoodsBean对象 */
  public GoodsBean buildGoodsBean(long playerId) {
    GoodsBean bean = new GoodsBean();
    bean.setGoodsId(getGoodsId());
    bean.setCommonGoods(buildCommonGoods(playerId));
    return bean;
  }

  public CommonGoodsBean buildCommonGoods(long playerId) {
    CommonGoodsBean bean = new CommonGoodsBean();
    bean.setGoodsType(getType());
    bean.setName(getName());
    bean.setDesc(getDesc());
    // 道具解析: rmb gold  diam 免费 广告
    bean.setPrice(getPriceBean());
    bean.setOriginPrice(getOriginPriceBean());
    bean.setDiscount(getDiscount());
    // 奖励解析
    bean.setRewards(getRewardsBeans());
    // 额外奖励解析
    bean.setExtraRewards(getExtraRewardsBeans());
    // 原奖励解析
    bean.setOriginRewards(getOriginRewardsBeans());
    // 限购类型
    bean.setLimitType(getResetType().getType());
    bean.setLimitCount(getLimitCount());
    // 当前玩家购买次数
    int count = PlayerBuyGoodsDOOperator.self().getCount(playerId, getGoodsId());
    bean.setAlreadyBuyTimes(count);
    bean.setSort(getSort());
    bean.setIcon(getIcon());
    bean.setTag(getTag());
    bean.setPlatform(getPlatform());
    bean.setChannel(getChannel());
    bean.setParam(getParam());
    bean.setStartBuyTime(getStartBuyTime());
    bean.setEndBuyTime(getEndBuyTime());
    return bean;
  }
}
