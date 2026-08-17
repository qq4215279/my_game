package com.mumu.game.buff;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.cxx.hf.config.script.buff.ConfigBuff;
import com.cxx.hf.config.script.buff.ConfigBuffActivityInfo;
import com.cxx.hf.config.script.buff.ConfigBuffBaseInfo;
import com.cxx.hf.config.script.buff.ConfigBuffEffectInfo;
import com.cxx.hf.config.script.buff.ConfigBuffGadGrantInfo;
import com.cxx.hf.config.script.buff.ConfigCardBuffInfo;
import com.cxx.hf.config.script.buff.ConfigCardInfo;
import com.cxx.hf.protocol.bean.player.element.CardBuffInfoBean;
import com.cxx.hf.util.collection.MapSubMap;
import com.cxx.hf.util.collection.MapUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.Getter;

/**
 *
 * buff 索引
 * @author: wangzhaoyuan
 * @date:   2018/1/5
 */
public class ConfigBuffIndex extends AbstractConfigIndex<ConfigBuff> {

    // 基本信息的map ,  key: id , value: info
    private ImmutableMap<Integer, ConfigBuffBaseInfo> baseMap ;
	@Getter
	private MapSubMap<Integer,Integer, List<ConfigBuffBaseInfo>> buffTypeFunctionBaseInfoMap;

	// Buff效果配置【通过BuffEffect关联BuffBaseInfo】
	private ImmutableMap<Integer, ConfigBuffEffectInfo> buffEffectMap ;

	/** 有卡片的buff **/
	@Getter
	private ImmutableMap<Integer, ConfigCardInfo> cardMap;
	/** 卡片所包含的buff **/
	@Getter
	private ImmutableMap<Integer, ConfigCardBuffInfo> cardBuffInfoMap;
	/** 卡片包含的buff **/
	private Map<Integer, List<ConfigCardBuffInfo>> cardContainBuffMap;

	@Getter
	private Map<Integer, CardBuffInfoBean> cardBuffInfoBeanMap;
	@Getter
	private Set<Integer> cardSet;

	// 神赐buff信息， key：type，value：infos
	@Getter
	private ImmutableMap<Integer, ImmutableList<ConfigBuffGadGrantInfo>> godGrantInfoMap;

	@Getter
	private Map<Integer, ConfigBuffGadGrantInfo> buffRewardMap;

	// 活动礼包增益加成
	@Getter
	private Map<Integer, List<ConfigBuffActivityInfo>> buffActivityInfos;

	/**      buff过滤条件-对应buffTypes **/
	@Getter
	private Map<Integer, Set<Integer>> sr2BuffTypeMap;
	
    public ConfigBuffIndex() {
        super("excelconfig/config_buff.xml");
    }

    @Override
    public void onLoadOver() {

        cleanListEleNullElements(scriptInst.getConfigBuffBase().getConfigBuffBaseInfo());
		cleanListEleNullElements(scriptInst.getConfigBuffGadGrant().getConfigBuffGadGrantInfo());
		cleanListEleNullElements(scriptInst.getConfigBuffActivity().getConfigBuffActivityInfo());

        baseMap = MapUtil.listToImmMap(scriptInst.getConfigBuffBase().getConfigBuffBaseInfo(), obj -> obj.getId().intValue());

		sr2BuffTypeMap = Maps.newHashMap();
		for (ConfigBuffBaseInfo baseInfo : baseMap.values()) {
			if (baseInfo.getBuffType().intValue() < 200 || baseInfo.getSubRequirements().isEmpty()){
				continue;
			}
			for (BigInteger subRequirement : baseInfo.getSubRequirements()) {
				Set<Integer> temp = sr2BuffTypeMap.computeIfAbsent(subRequirement.intValue(), v -> Sets.newHashSet());
				temp.add(baseInfo.getBuffType().intValue());
			}
		}
		buffTypeFunctionBaseInfoMap = MapUtil.listToMapSubListMap(scriptInst.getConfigBuffBase().getConfigBuffBaseInfo(),obj -> obj.getBuffType().intValue(),obj -> obj.getAddType().intValue());

		buffEffectMap = MapUtil.listToImmMap(scriptInst.getConfigBuffEffect().getConfigBuffEffectInfo(), obj -> obj.getBuffEffectId().intValue());

		cardMap = MapUtil.listToImmMap(scriptInst.getConfigCard().getConfigCardInfo(), obj -> obj.getItemId().intValue());
		cardBuffInfoMap = MapUtil.listToImmMap(scriptInst.getConfigCardBuff().getConfigCardBuffInfo(), obj -> obj.getId().intValue());
		cardContainBuffMap = Maps.newHashMapWithExpectedSize(cardMap.size());
		cardSet = Sets.newHashSetWithExpectedSize(cardMap.size());
		for (ConfigCardInfo cardInfo : cardMap.values()) {
			int itemId = cardInfo.getItemId().intValue();
			for (BigInteger buffId : cardInfo.getBuffId()) {
				ConfigCardBuffInfo buffInfo = cardBuffInfoMap.get(buffId.intValue());
				if (buffInfo == null) {
					continue;
				}
				cardContainBuffMap.computeIfAbsent(itemId, k -> Lists.newArrayList()).add(buffInfo);
			}
			cardSet.add(itemId);
		}

		cardBuffInfoBeanMap = Maps.newHashMapWithExpectedSize(cardBuffInfoMap.size());
		for (ConfigCardBuffInfo buffInfo : cardBuffInfoMap.values()) {
			CardBuffInfoBean bean = new CardBuffInfoBean();
			bean.setBuffId(buffInfo.getId().intValue());
			bean.setName(buffInfo.getName());
			bean.setType(buffInfo.getType().intValue());
			bean.setAddValue(buffInfo.getValue().longValue());
			bean.setShortMessage(buffInfo.getMessage());
			cardBuffInfoBeanMap.put(buffInfo.getId().intValue(), bean);
		}

		godGrantInfoMap = MapUtil.listToImmMapWithList(scriptInst.getConfigBuffGadGrant().getConfigBuffGadGrantInfo(), o -> o.getType().intValue());

		buffRewardMap = Maps.newHashMap();
		for (ConfigBuffGadGrantInfo configBuffGadGrantInfo : scriptInst.getConfigBuffGadGrant().getConfigBuffGadGrantInfo()) {
			if (!configBuffGadGrantInfo.getGoldNum().isEmpty()) {
				buffRewardMap.putIfAbsent(configBuffGadGrantInfo.getBuffId().intValue(), configBuffGadGrantInfo);
			}
		}

		buffActivityInfos = Maps.newHashMap();
		for (ConfigBuffActivityInfo configBuffActivityInfo : scriptInst.getConfigBuffActivity().getConfigBuffActivityInfo()) {
			//1代表开启
			if (configBuffActivityInfo.getOpen().intValue() != 1) {
				continue;
			}
			for (BigInteger shopId : configBuffActivityInfo.getShopId()) {
				List<ConfigBuffActivityInfo> list = buffActivityInfos.computeIfAbsent(shopId.intValue(), k -> Lists.newArrayList());
				list.add(configBuffActivityInfo);
			}
		}

	}

    /**
     * 获取一个 buff 配置数据
     * @param id
     * @return
     */
    public ConfigBuffBaseInfo getBuff(int id) {
        return baseMap.get(id);
    }

    /**
     * 获取buff列表
     * @param buffType buff类型
     * @return java.util.List<com.cxx.hf.config.script.buff.ConfigBuffBaseInfo>
     * @date 2024/7/1 14:11
     */
    public List<ConfigBuffBaseInfo> getConfigBuffBaseInfoListByBuffType(int buffType) {
		return baseMap.values().stream().filter(e -> e.getBuffType().intValue() == buffType).collect(Collectors.toList());
	}

	@Override
	public void checkBeforeLoad() throws Exception {
		// Auto-generated method stub
		
	}

	/**
	 * 获取一个 ConfigBuffEffectInfo 配置数据
	 * @param effectId
	 * @return
	 */
	public ConfigBuffEffectInfo getBuffEffectInfo(int effectId) {
		return buffEffectMap.get(effectId);
	}

	public List<ConfigCardBuffInfo> getCardContainBuffMap(int cardId) {
		if (cardContainBuffMap.get(cardId) == null) {
			return Collections.emptyList();
		}
		return cardContainBuffMap.get(cardId);
	}

	/**
	 * 获取卡片对应所有类型的buff对应的的累积值
	 * @param cards
	 * @return
	 */
	public Map<Integer, Long> getCardBuffEffectTimes(Set<Integer> cards) {
		Map<Integer, Long> retMap = Maps.newHashMap();
		for (Integer cardId : cards) {
			getCardContainBuffMap(cardId)
					.forEach(k -> retMap.put(k.getType().intValue(), retMap.getOrDefault(k.getType().intValue(), 0L) + k.getValue().intValue()));
		}
		return retMap;
	}

	public boolean compareBuffPriority(int buffId1, int buffId2) {
		ConfigBuffBaseInfo buff1 = getBuff(buffId1);
		ConfigBuffBaseInfo buff2 = getBuff(buffId2);
		return buff1.getGroupPriority().intValue() > buff2.getGroupPriority().intValue();
	}

}

