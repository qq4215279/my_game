package com.mumu.game.buff;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;

import com.cxx.hf.config.index.ConfigBuffIndex;
import com.cxx.hf.config.index.ConfigSundryIndex;
import com.cxx.hf.config.inf.IConfigIndexProvider;
import com.cxx.hf.config.script.buff.ConfigBuffBaseInfo;
import com.cxx.hf.config.script.buff.ConfigBuffEffectInfo;
import com.cxx.hf.domain.common.ContentProvider;
import com.cxx.hf.domain.constants.BuffConstants;
import com.cxx.hf.domain.constants.ModelConstant;
import com.cxx.hf.domain.entity.po.player.BuffDailyInfo;
import com.cxx.hf.domain.entity.po.player.BuffEffectInfo;
import com.cxx.hf.domain.entity.po.player.BuffInfo;
import com.cxx.hf.domain.entity.po.player.PlayerBuffCardInfo;
import com.cxx.hf.domain.event.player.PlayerBuffChangeEvent;
import com.cxx.hf.domain.event.player.PlayerBuffEffectEvent;
import com.cxx.hf.domain.log.FeaturesLogger;
import com.cxx.hf.domain.model.AbstractModel;
import com.cxx.hf.protocol.bean.player.common.BuffInfoBean;
import com.cxx.hf.servercore.pool.PooledObjectFactory;
import com.cxx.hf.servercore.redis.RedisTemplate;
import com.cxx.hf.util.collection.ListUtil;
import com.cxx.hf.util.collection.MapSubMap;
import com.cxx.hf.util.date.DateUtil;
import com.cxx.hf.util.inf.IArgumentRunnable;
import com.cxx.hf.util.inf.IDataProvider;
import com.cxx.hf.util.math.RandomUtil;
import com.cxx.hf.util.other.ChangeAbleArg;
import com.cxx.hf.util.other.Checker;
import com.cxx.hf.util.other.DataPair;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import cn.hutool.core.util.NumberUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * buff model
 */
@Slf4j
public class BuffModel extends AbstractModel {
	public BuffModel(Long userId, RedisTemplate lt, RedisTemplate redisTemplate, Boolean lock, Boolean noOnLoadOver) {
		super(ModelConstant.KEY_BUFF, userId, lt, redisTemplate, lock, noOnLoadOver);
	}

	@Override
	protected void init() {
		this.data.initNewSubMap(ModelConstant.DATA_KEY_BUFF); // buff id - buff info
		this.data.initNewSubMap(ModelConstant.DATA_KEY_BUFF_EFFECT); // buff id - buffeffect info
		this.data.set(ModelConstant.DATA_KEY_BUFF_DAILY, new BuffDailyInfo());
	}

	@Override
	public void onLoadOver() {

		if (!this.data.isExists(ModelConstant.DATA_KEY_BUFF_EFFECT)) {
			this.data.initNewSubMap(ModelConstant.DATA_KEY_BUFF_EFFECT);
		}

		// 检查过期的BuffInfo
		List<Integer> removeBuffIds = checkExpireBuffInfo();
		// 检查过期的BuffEffectInfo
		List<Integer> removeEffectBuffIds = checkExpireBuffEffectInfo();
		// buff删除事件
		if (CollectionUtils.isNotEmpty(removeBuffIds) || CollectionUtils.isNotEmpty(removeEffectBuffIds)){
			postBuffChangeEvent(getUniqueId(), null, ListUtils.union(removeBuffIds, removeEffectBuffIds));
		}

		// 检测每日的数据
		BuffDailyInfo buffDailyInfo = getBuffDailyInfo();
		if (!DateUtil.isToday(buffDailyInfo.getLastUpdateTime())) {
			buffDailyInfo.reset();
		}

		// 不受赛季控制
		PlayerBuffCardInfo buffCardCardInfo = getPlayerBuffCardInfo();
		if (buffCardCardInfo == null) {
			initPlayerBuffCardInfo();
		} else {
			buffCardCardInfo.reset();
		}
	}

	public Map<Integer,List<Integer>> getAllRedDiamond() {
		Map<Integer,List<Integer>> map = Maps.newHashMapWithExpectedSize(2);
		ConfigBuffIndex configBuffIndex = ContentProvider.getConfigIndexProvider().getConfigIndex(IConfigIndexProvider.CONFIG_BUFF);
		List<ConfigBuffBaseInfo> configBuffBaseInfos = configBuffIndex.getConfigBuffBaseInfoListByBuffType(BuffConstants.BUFF_TYPE_NEW_GOLDBOX);
		for (ConfigBuffBaseInfo configBuffBaseInfo : configBuffBaseInfos) {
			if (!hasBuffByConfigId(configBuffBaseInfo.getId().intValue())) {
				continue;
			}
			for (int i = 0; i < 10; i++) {
				int newBuffId = configBuffBaseInfo.getGroup().intValue() + i;
				ConfigBuffBaseInfo newBuff = configBuffIndex.getBuff(newBuffId);
				if (newBuff == null || hasBuffByConfigId(newBuffId)) {
					continue;
				}
				List<Integer> buffList = map.computeIfAbsent(configBuffBaseInfo.getId().intValue(),v -> Lists.newArrayList());
				buffList.add(newBuffId);
			}
		}
		return map;
	}

	/**
	 * 检查并移除过期的BuffInfo
	 * @return
	 */
	public List<Integer> checkExpireBuffInfo() {
		final long now = System.currentTimeMillis();
		List<Integer> removeBuffIds = Lists.newArrayListWithExpectedSize(1);
		this.data.<Integer, BuffInfo> foreachSubMap(ModelConstant.DATA_KEY_BUFF, iter -> {
			BuffInfo buffInfo = iter.next().getValue();
			if (buffInfo == null || buffInfo.isTimeout(now) || buffReset(buffInfo)) {
				iter.remove();

				if (isLocked()) {
					FeaturesLogger.logBuff(this.uniqueId, 0, buffInfo.getConfigId(), "del");
					removeBuffIds.add(buffInfo.getConfigId());
				}
			}
		});
		return removeBuffIds;
	}

	/**
	 * 检查并移除过期的BuffEffectInfo
	 * @return
	 */
	public List<Integer> checkExpireBuffEffectInfo() {
		final long now = System.currentTimeMillis();
		List<Integer> removeBuffIds = Lists.newArrayListWithExpectedSize(1);
		this.data.<Integer, BuffEffectInfo> foreachSubMap(ModelConstant.DATA_KEY_BUFF_EFFECT, iter -> {
			BuffEffectInfo buffEffectInfo = iter.next().getValue();
			if (buffEffectInfo != null) {
				final List<BuffInfo> buffInfos = buffEffectInfo.toBuffInfos(false);
				int invalidCount = 0;
				for (BuffInfo buffInfo : buffInfos) {
					if (buffInfo == null || buffInfo.isTimeout(now) || buffReset(buffInfo)) {
						invalidCount ++;
						if (isLocked()) {
							FeaturesLogger.logBuff(this.uniqueId, buffEffectInfo.getEffectId(), buffInfo.getConfigId(), "del");
							removeBuffIds.add(buffInfo.getConfigId());
						}
					}
				}
				if (buffInfos.size() == invalidCount) {
					iter.remove();
				}
			} else {
				iter.remove();
				FeaturesLogger.logBuff(this.uniqueId, buffEffectInfo.getEffectId(), 0, "del");
			}
		});
		return removeBuffIds;
	}

	/**
	 * 获取buff的每日信息
	 * @return
	 */
	public BuffDailyInfo getBuffDailyInfo() {
		return this.data.get(ModelConstant.DATA_KEY_BUFF_DAILY);
	}

	/**
	 * 获取玩家所有有效buff
	 * @return
	 */
	public Collection<BuffInfo> getAllBuff() {
				Collection<BuffInfo> orgBuffInfos = this.data.getSubMapItems(ModelConstant.DATA_KEY_BUFF);
				Collection<BuffEffectInfo> buffEffects = this.data.getSubMapItems(ModelConstant.DATA_KEY_BUFF_EFFECT);
				if (CollectionUtils.isEmpty(buffEffects)) {
					return orgBuffInfos;
				}
				// 合并 orgBuffInfos 和 buffEffectInfo 中的 BuffInfo
				List<BuffInfo> buffInfos = Lists.newArrayList(orgBuffInfos);
				for (BuffEffectInfo buffEffect : buffEffects) {
					buffInfos.addAll(buffEffect.toBuffInfos(true));
				}
				// 遍历比较一下 过滤
				ConfigBuffIndex configBuffIndex = ContentProvider.getConfigIndexProvider().getConfigIndex(IConfigIndexProvider.CONFIG_BUFF);
				MapSubMap<Integer, Integer, BuffInfo> allBuffMapSubMap = new MapSubMap<>();
				for (BuffInfo buffInfo : buffInfos) {
					ConfigBuffBaseInfo configBuffBaseInfo = configBuffIndex.getBuff(buffInfo.getConfigId());
					if(configBuffBaseInfo==null){
						continue;
					}
					Map<Integer, BuffInfo> map = allBuffMapSubMap.getOrCreateSubMap(configBuffBaseInfo.getBuffType().intValue());
					map.merge(configBuffBaseInfo.getGroup().intValue(), buffInfo,
							(buff1, buff2) -> configBuffIndex.compareBuffPriority(buff1.getConfigId(), buff2.getConfigId()) ? buff1 : buff2);
		}
		return allBuffMapSubMap.allValues();
	}

	/**
	 * buff删除重置
	 * 根据杂项配置 清除buff创建时间比配置时间小的buff
	 * @param buffInfo
	 * @return
	 */
	private boolean buffReset(BuffInfo buffInfo) {
		ConfigSundryIndex sundryIndex = ContentProvider.getConfigIndexProvider().getConfigIndex(IConfigIndexProvider.CONFIG_SUNDRY);
		ConfigBuffIndex configBuffIndex = ContentProvider.getConfigIndexProvider().getConfigIndex(IConfigIndexProvider.CONFIG_BUFF);
		int buffId = buffInfo.getConfigId();
		ConfigBuffBaseInfo buff = configBuffIndex.getBuff(buffId);
		if (buff == null) {
			return false;
		}
		int buffType = buff.getBuffType().intValue();
		if (sundryIndex.getGodGrantBuffResetTypes().contains(buffType)
				&& System.currentTimeMillis() >= sundryIndex.getGodGrantBuffResetTime()
				&& sundryIndex.getGodGrantBuffResetTime() > buffInfo.getCreateTime()) {
			return true;
		}

		return false;
	}

	/**
	 * 添加一个buff 并插入buff的效果值 （此方法暂时适用于每日寻宝后的奖励领取中） add by xiangneng
	 * @param configId
	 * @param buffValue
	 */
	public void addBuff(int configId, float buffValue) {
		addBuff(configId);

		BuffInfo buffInfo = getBuffInfo(configId);
		if (buffInfo != null) {
			if (buffValue > buffInfo.getBuffValue()) {
				buffInfo.setBuffValue(buffValue);
			}
		}
	}

	/**
	 * 添加一个buff
	 * @param configId
	 */
	public void addBuff(int configId) {
		ConfigBuffIndex configBuffIndex = ContentProvider.getConfigIndexProvider().getConfigIndex(IConfigIndexProvider.CONFIG_BUFF);
		ConfigBuffBaseInfo buff = configBuffIndex.getBuff(configId);
		if (buff == null) {
			throw new RuntimeException("error buff configId: " + configId + " , no config data.");
		}

		BuffInfo buffInfo = new BuffInfo();
		buffInfo.setConfigId(configId);
		List<BuffInfoBean> addBuffIds = null;
		List<Integer> removeBuffIds = null;

		BuffInfo oldBuff = this.data.getSubMapItem(ModelConstant.DATA_KEY_BUFF, configId);
		if (oldBuff != null) {
			// 讲原来的值保留 方便外部判断(目前只限于每日寻宝 类型28的判断)
			buffInfo.setBuffValue(oldBuff.getBuffValue());
			if (log.isDebugEnabled())
				log.debug("buff(configId={}) will be override: oldBuff info: {}", configId, oldBuff);
		} else {
			int buffGroup = buff.getGroup().intValue();
			if (buffGroup > 0) {
				// 需要检测buff的组
				Iterator<BuffInfo> iterator = this.data.<BuffInfo> getSubMapItems(ModelConstant.DATA_KEY_BUFF).iterator();
				while (iterator.hasNext()) {
					BuffInfo item = iterator.next();
					ConfigBuffBaseInfo t = configBuffIndex.getBuff(item.getConfigId());
					if (t.getGroup().intValue() == buffGroup) {
						// 相同组， 判定优先级
						if (t.getGroupPriority().intValue() < buff.getGroupPriority().intValue()) {
							// 存在的buff 的组优先级小于 当前的
							// 顶掉他
							iterator.remove();
							if (removeBuffIds == null){
								removeBuffIds = Lists.newArrayListWithExpectedSize(1);
							}
							removeBuffIds.add(t.getId().intValue());
						} else {
							// 优先级较小， 不顶掉原来的buff
							return;
						}
					}
				}
			}
		}

		// 设置持续时间
		long duration = 0L;
		int dType = buff.getDurationType().intValue();
		if (dType == 1) {
			duration = DateUtil.getTomorrowStartTime().getTime() - System.currentTimeMillis();
		} else if (dType == 2) {
			duration = buff.getDuration().intValue() * 60 * 1000L;
//			if (buff.getBuffType().intValue() == BuffConstants.BUFF_TYPE_IMMORTAL_KILL_FISH_EXP_ADD) {
//				// 特殊buff 如果是修仙场，需要获取对应的
//				ConfigRegulateIndex regulateIndex = ContentProvider.getConfigIndexProvider().getConfigIndex(IConfigIndexProvider.CONFIG_REGULATE);
//				// buff真实时间为 对应的默认时间-去活动开启后的指定时间
//				duration -= regulateIndex.getActivityPassTimes(CommonConstants.ACTIVITY_FISHERY_IMMORTALS_TYPE);
//			}
			// buff可叠加时间
			if (buff.getSuperpositionType().intValue() == 1 && oldBuff != null) {
				// 旧的buff剩余时间
				long oldTime = oldBuff.getDuration() - (System.currentTimeMillis() - oldBuff.getCreateTime());
				oldTime = oldTime < 0 ? 0 : oldTime;
				duration += oldTime;
			}
		} else {
			throw new RuntimeException("error DurationType(" + dType + ") buff configId: " + configId);
		}
		buffInfo.setDuration(duration);
		this.data.addSubMapItem(ModelConstant.DATA_KEY_BUFF, configId, buffInfo);
		FeaturesLogger.logBuff(this.uniqueId, 0, configId, "add");
		addBuffIds = Lists.newArrayListWithExpectedSize(1);
		addBuffIds.add(buffInfo.toBean());
		// 丢一个事件出去,处理其他效果的部分
		postBuffEffectEvent(buffInfo);
		postBuffChangeEvent(getUniqueId(),addBuffIds, removeBuffIds);
	}

	private void postBuffEffectEvent(BuffInfo buffInfo) {
		PlayerBuffEffectEvent event = PooledObjectFactory.getInstance().borrowObject(PlayerBuffEffectEvent.class);
		event.setBuffConfigId(buffInfo.getConfigId());
		event.setPlayerId(this.uniqueId);
		event.setExpirationTime(buffInfo.getCreateTime() + buffInfo.getDuration());
		ContentProvider.postAsyncEvent(event);
	}

	public void addBuffEffect(int effectId) {
		ConfigBuffIndex configBuffIndex = ContentProvider.getConfigIndexProvider().getConfigIndex(IConfigIndexProvider.CONFIG_BUFF);
		ConfigBuffEffectInfo configBuffEffectInfo = configBuffIndex.getBuffEffectInfo(effectId);
		if (configBuffEffectInfo == null) {
			throw new RuntimeException("error configBuffEffectInfo effectId: " + effectId + " , no config data.");
		}
		BuffEffectInfo oldBuff = this.data.getSubMapItem(ModelConstant.DATA_KEY_BUFF_EFFECT, effectId);
		if (oldBuff != null) {
			return;
		}
		BuffEffectInfo buffEffectInfo = new BuffEffectInfo(effectId);
		this.data.addSubMapItem(ModelConstant.DATA_KEY_BUFF_EFFECT, effectId, buffEffectInfo);
		// 相关通知Event
		List<BuffInfoBean> addBuffIds = Lists.newArrayListWithExpectedSize(1);
		for (BuffInfo buffInfo : buffEffectInfo.toBuffInfos(true)) {
			addBuffIds.add(buffInfo.toBean());
			// 丢一个事件出去,处理其他效果的部分
			postBuffEffectEvent(buffInfo);
			FeaturesLogger.logBuff(this.uniqueId, effectId, buffInfo.getConfigId(), "add");
		}
		postBuffChangeEvent(getUniqueId(), addBuffIds, null);
	}

	/**
	 * 移除buffEffect
	 * @param buffEffectId
	 */
	public void removeBuffEffect(int buffEffectId) {
		this.data.getSubMap(ModelConstant.DATA_KEY_BUFF_EFFECT).remove(buffEffectId);
		ConfigBuffIndex configBuffIndex = ContentProvider.getConfigIndexProvider().getConfigIndex(IConfigIndexProvider.CONFIG_BUFF);
		ConfigBuffEffectInfo configBuffEffectInfo = configBuffIndex.getBuffEffectInfo(buffEffectId);
		if (configBuffEffectInfo != null) {
			List<Integer> removeBuffIds = Lists.newArrayListWithExpectedSize(1);
			configBuffEffectInfo.getBuffIds().forEach(buff -> removeBuffIds.add(buff.intValue()));
			postBuffChangeEvent(getUniqueId(), null, removeBuffIds);
		}

	}

	// 上传buff变更事件
	private void postBuffChangeEvent(long playerId,List<BuffInfoBean> addBuffIds, List<Integer> removeBuffIds) {
		if (ListUtil.isEmptyOrNull(addBuffIds) && ListUtil.isEmptyOrNull(removeBuffIds)) {
			return;
		}
		PlayerBuffChangeEvent buffChangeEvent = PooledObjectFactory.getInstance().borrowObject(PlayerBuffChangeEvent.class);
		buffChangeEvent.setPlayerId(playerId);
		buffChangeEvent.setAddBuffIds(addBuffIds);
		buffChangeEvent.setRemoveBuffIds(removeBuffIds);
		ContentProvider.postAsyncEvent(buffChangeEvent);
	}

	/**
	 * @param type
	 * @param baseValueRef
	 * @return
	 */
	public double getBuffEffectValue(final int type, ChangeAbleArg<Integer> baseValueRef) {
		return getBuffEffectValue(type, baseValueRef, null);
	}

	public boolean hasBuffByConfigId(int configId){
		BuffInfo oldBuff = this.data.getSubMapItem(ModelConstant.DATA_KEY_BUFF, configId);
		if (oldBuff != null){
			return true;
		}
		return false;
	}

	public boolean hasBuffEffectByConfigId(int configId) {
		BuffEffectInfo oldBuff = this.data.getSubMapItem(ModelConstant.DATA_KEY_BUFF_EFFECT, configId);
		if (oldBuff != null) {
			return true;
		}
		return false;
	}

	/***
	 * 获取BUFF加成值
	 * @param type
	 * @param requireCheck
	 * @return
	 */
	public int getBuffPercentValue(final int type, IDataProvider<Boolean, ConfigBuffBaseInfo> requireCheck) {
		ConfigBuffIndex index = ContentProvider.getConfigIndexProvider().getConfigIndex(IConfigIndexProvider.CONFIG_BUFF);
		return getBuffsByType(type, requireCheck).stream().map(index::getBuff).mapToInt(info -> info == null ? 0 :info.getPercent().intValue()).sum();
	}

	public double getBuffEffectValue(final int type, ChangeAbleArg<Integer> baseValueRef, IDataProvider<Boolean, ConfigBuffBaseInfo> requireCheck) {
		long value = baseValueRef.getVal();
		ChangeAbleArg<Long> baseValue = new ChangeAbleArg<Long>(value);
		double ret = getBuffEffectLongValue(type, baseValue, requireCheck);
		baseValueRef.setVal(baseValue.getVal().intValue());
		return ret;
	}

	/**
	 * @param type
	 * @param baseValueRef
	 * @return
	 */
	public double getBuffEffectLongValue(final int type, ChangeAbleArg<Long> baseValueRef) {
		return getBuffEffectLongValue(type, baseValueRef, null);
	}

	/**
	 * 获取到buff类型的效果值
	 * @param type
	 * @param baseValueRef 基础值
	 * @return
	 */
	public double getBuffEffectLongValue(final int type, ChangeAbleArg<Long> baseValueRef, IDataProvider<Boolean, ConfigBuffBaseInfo> requireCheck) {

		long baseValue = baseValueRef.getVal();
		// 蟹将积分和船长积分
		boolean flag1 = false;
		// check 保底
		if (type == BuffConstants.BUFF_TYPE_CRAB_REWARD_VALUE || type == BuffConstants.BUFF_TYPE_GHOST_SHIP
				|| type == BuffConstants.BUFF_TYPE_MISSILE_EDEN_SCORE_UP) {
			flag1 = true;
		}

		double newBaseValue;
		if (flag1) {
			ChangeAbleArg<Integer> max = new ChangeAbleArg<>(0);
			ChangeAbleArg<Integer> min = new ChangeAbleArg<>(0);

			DataPair<Double, Double> dataPair = getBuffEffectValue(type, requireCheck, obj -> {
				if (obj.getValues().size() >= 2) {
					int t1 = obj.getValues().get(0).intValue();
					int t2 = obj.getValues().get(1).intValue(); // 大的
					// 2018.01.24 经过和策划讨论，数值冲突了，使用最大值大的那个
					if (max.getVal() < t2) {
						max.setVal(t2);
						min.setVal(t1);
					}
				}
			});

			if (baseValue < max.getVal()) {
				int x = RandomUtil.random(min.getVal(), max.getVal());
				baseValue = Math.max(baseValue, x);
				baseValueRef.setVal(baseValue);
			}

			if (dataPair == null) {
				return baseValue;
			}

			newBaseValue = NumberUtil.mul(baseValue , (1 + dataPair.getObj1())) + dataPair.getObj2();
		} else {
			DataPair<Double, Double> dataPair = getBuffEffectValueByRequire(type, requireCheck);
			if (dataPair == null) {
				return baseValue;
			}

			newBaseValue = NumberUtil.mul(baseValue , (1 + dataPair.getObj1())) + dataPair.getObj2();
		}
		return newBaseValue;
	}

	public double getDeBuffEffectValue(final int type, ChangeAbleArg<Integer> baseValueRef, IDataProvider<Boolean, ConfigBuffBaseInfo> requireCheck) {
		long value = baseValueRef.getVal();
		ChangeAbleArg<Long> baseValueLong = new ChangeAbleArg<>(value);
		double ret = getDeBuffEffectLongValue(type, baseValueLong, requireCheck);
		baseValueRef.setVal(baseValueLong.getVal().intValue());
		return ret;
	}

	/**
	 * 获取到buff类型的减益效果值
	 * @param type
	 * @param baseValueRef 基础值
	 * @return
	 */
	public double getDeBuffEffectLongValue(final int type, ChangeAbleArg<Long> baseValueRef, IDataProvider<Boolean, ConfigBuffBaseInfo> requireCheck) {

		Long baseValue = baseValueRef.getVal();
		// 蟹将积分和船长积分
		boolean flag1 = false;
		// check 保底
		if (type == BuffConstants.BUFF_TYPE_CRAB_REWARD_VALUE || type == BuffConstants.BUFF_TYPE_GHOST_SHIP
				|| type == BuffConstants.BUFF_TYPE_MISSILE_EDEN_SCORE_UP) {
			flag1 = true;
		}

		double newBaseValue;
		if (flag1) {
			ChangeAbleArg<Integer> max = new ChangeAbleArg<>(0);
			ChangeAbleArg<Integer> min = new ChangeAbleArg<>(0);

			DataPair<Double, Double> dataPair = getBuffEffectValue(type, requireCheck, obj -> {
				if (obj.getValues().size() >= 2) {
					int t1 = obj.getValues2().get(0).intValue();
					int t2 = obj.getValues2().get(1).intValue(); // 大的
					// 2018.01.24 经过和策划讨论，数值冲突了，使用最大值大的那个
					if (max.getVal() < t2) {
						max.setVal(t2);
						min.setVal(t1);
					}
				}
			});

			if (baseValue > min.getVal()) {
				int x = RandomUtil.random(min.getVal(), max.getVal());
				baseValue = Math.min(baseValue, x);
				baseValueRef.setVal(baseValue);
			}

			if (dataPair == null) {
				return baseValue;
			}
			newBaseValue = baseValue * (1 - dataPair.getObj1()) - dataPair.getObj2();
		} else {
			DataPair<Double, Double> dataPair = getBuffEffectValueByRequire(type, requireCheck);
			if (dataPair == null) {
				return baseValue;
			}
			newBaseValue = baseValue * (1 - dataPair.getObj1()) - dataPair.getObj2();
		}
		return newBaseValue;
	}

	/**
	 * @param type
	 * @return obj1: 百分比加成(0~1) , obj2: 值加成
	 */
	public DataPair<Double, Double> getBuffEffectValue(final int type) {
		return getBuffEffectValueByRequire(type, null);
	}

	public DataPair<Double, Double> getBuffEffectValueByRequire(final int type, IDataProvider<Boolean, ConfigBuffBaseInfo> requireCheck) {
		return getBuffEffectValue(type, requireCheck, null);
	}

	/**
	 * @param type
	 * @param requireCheck 条件检查
	 * @return obj1: 百分比加成(0~1) , obj2: 值加成
	 */
	public DataPair<Double, Double> getBuffEffectValue(final int type, IDataProvider<Boolean, ConfigBuffBaseInfo> requireCheck,
			IArgumentRunnable<ConfigBuffBaseInfo> doAfterAddValue) {
		return getBuffEffectValueByFunctionId(type, 0, requireCheck, doAfterAddValue);
	}
	/**
	 * @param type
	 * @param requireCheck 条件检查
	 * @return obj1: 百分比加成(0~1) , obj2: 值加成
	 */
	public DataPair<Double, Double> getBuffEffectValueByFunctionId(final int buffType,int functionId, IDataProvider<Boolean, ConfigBuffBaseInfo> requireCheck,
			IArgumentRunnable<ConfigBuffBaseInfo> doAfterAddValue) {

		// 单个buff生效map  标记index:value<被比较值, ConfigBuffBaseInfo>
		Map<Integer, DataPair<Double, ConfigBuffBaseInfo>> markIndexValueMap = new HashMap<>(1);

		if (this.data.isSubMapEmpty(ModelConstant.DATA_KEY_BUFF) && this.data.isSubMapEmpty(ModelConstant.DATA_KEY_BUFF_EFFECT)) {
			return null;
		}

		double percent = 0;
		double value = 0;

		final ConfigBuffIndex configBuffIndex = ContentProvider.getConfigIndexProvider().getConfigIndex(IConfigIndexProvider.CONFIG_BUFF);
		for (BuffInfo buffInfo : getAllBuff()) {
			ConfigBuffBaseInfo buffBaseInfo = configBuffIndex.getBuff(buffInfo.getConfigId());
			if (buffBaseInfo == null){
				continue;
			}
			if (functionId != 0 && functionId != buffBaseInfo.getFunctionId().intValue()){
				continue;
			}
			if (buffBaseInfo.getBuffType().intValue() == buffType) {
				if (requireCheck != null) {
					if (!Checker.isTrue(requireCheck.getData(buffBaseInfo))) {
						continue; // 条件不满足， 忽略
					}
				}

				BigInteger markSingleEffect = buffBaseInfo.getMarkSingleEffect();
				if (!checkNeedMerge(markSingleEffect)) {
					percent += buffBaseInfo.getPercent().doubleValue();
					value += buffBaseInfo.getValue().doubleValue();

					if (doAfterAddValue != null) {
						doAfterAddValue.run(buffBaseInfo);
					}
				} else {
					// <被比较值, ConfigBuffBaseInfo>
					DataPair<Double, ConfigBuffBaseInfo> dataPair = markIndexValueMap.computeIfAbsent(markSingleEffect.intValue(), k -> new DataPair<>(0D, buffBaseInfo));
					double singleValue = getCompareValue(buffBaseInfo);
					if (singleValue > dataPair.getObj1()) {
						dataPair.setObj1(singleValue);
						dataPair.setObj2(buffBaseInfo);
					}

				}

			}
		}

		// 汇总加成值
		for (DataPair<Double, ConfigBuffBaseInfo> dataPair : markIndexValueMap.values()) {
			ConfigBuffBaseInfo buffBaseInfo = dataPair.getObj2();
			percent += buffBaseInfo.getPercent().doubleValue();
			value += buffBaseInfo.getValue().doubleValue();

			if (doAfterAddValue != null) {
				doAfterAddValue.run(buffBaseInfo);
			}
		}

		if (percent == 0 && value == 0) {
			return null;
		}

		percent /= 100; // 策划表里面填写的 不是小数

		return DataPair.fromTwo(percent, value);
	}

	/**
	 * 是否有这种类型的buff
	 * @param type
	 * @return
	 */
	public boolean isHas(int type, IDataProvider<Boolean, ConfigBuffBaseInfo> requireCheck) {

		final ConfigBuffIndex configBuffIndex = ContentProvider.getConfigIndexProvider().getConfigIndex(IConfigIndexProvider.CONFIG_BUFF);
		for (BuffInfo buffInfo : getAllBuff()) {
			ConfigBuffBaseInfo buffBaseInfo = configBuffIndex.getBuff(buffInfo.getConfigId());
			if (buffBaseInfo == null){
				log.error("buff {} not exist,please check config ",buffInfo.getConfigId());
				continue;
			}
			if (buffBaseInfo.getBuffType().intValue() == type) {
				if (requireCheck != null) {
					if (!Checker.isTrue(requireCheck.getData(buffBaseInfo))) {
						continue;
					}
				}
				return true;
			}
		}

		return false;
	}

	/**
	 * 获取玩家指定渔场的狂暴倍率系数Buff
	 * @param gameId
	 * @return
	 */
	public int getCrazySkillRadio(int gameId) {
		final ConfigBuffIndex buffIndex = ContentProvider.getConfigIndexProvider().getConfigIndex(IConfigIndexProvider.CONFIG_BUFF);
		for (BuffInfo buffInfo : getAllBuff()) {

			ConfigBuffBaseInfo configBuff = buffIndex.getBuff(buffInfo.getConfigId());
			if (configBuff == null){
				continue;
			}
			if (configBuff.getBuffType().intValue() == BuffConstants.BUFF_TYPE_CRAZY_UP && gameId == configBuff.getRequirement().intValue()) {
				return configBuff.getValue().intValue();
			}
		}
		return 0;
	}

	/**
	 * 获取所有指定类型buff
	 * @param type
	 * @param requireCheck
	 * @return
	 */
	public List<Integer> getBuffsByType(int type, IDataProvider<Boolean, ConfigBuffBaseInfo> requireCheck) {
		List<Integer> buffs = Lists.newArrayListWithExpectedSize(1);
		final ConfigBuffIndex configBuffIndex = ContentProvider.getConfigIndexProvider().getConfigIndex(IConfigIndexProvider.CONFIG_BUFF);

		// 单个buff生效map  标记index:DataPair<value, ConfigBuffBaseInfo.id>
		Map<Integer, DataPair<Double, Integer>> markIndexValueMap = new HashMap<>(1);

		for (BuffInfo buffInfo : getAllBuff()) {
			ConfigBuffBaseInfo buffBaseInfo = configBuffIndex.getBuff(buffInfo.getConfigId());
			if(buffBaseInfo==null){
				log.error("getBuffsByType config is null uid:{} id:{}",this.getUniqueId(),buffInfo.getConfigId());
				continue;
			}
			if (buffBaseInfo.getBuffType().intValue() == type) {
				if (requireCheck != null) {
					if (!Checker.isTrue(requireCheck.getData(buffBaseInfo))) {
						continue;
					}
				}

				if (!checkNeedMerge(buffBaseInfo.getMarkSingleEffect())) {
					buffs.add(buffBaseInfo.getId().intValue());
				} else {
					putMarkIndexValueMap(markIndexValueMap, buffBaseInfo);
				}

			}
		}

		// 汇总加成buff列表
		buffs.addAll(markIndexValueMap.values().stream().map(o -> o.getObj2()).collect(Collectors.toList()));

		return buffs;
	}

	/**
	 * 获取一个buff
	 * @param configId
	 * @return
	 */
	public BuffInfo getBuffInfo(int configId) {
		BuffInfo buffInfo = this.data.getSubMapItem(ModelConstant.DATA_KEY_BUFF, configId);
		return buffInfo;
	}

	/**
	 * 移除buff
	 * @param buffId
	 */
	public void removeBuff(int buffId) {
		this.data.getSubMap(ModelConstant.DATA_KEY_BUFF).remove(buffId);
		postBuffChangeEvent(getUniqueId(),null, Lists.newArrayList(buffId));
	}

	/**
	 * 获取所有指定类型buff
	 * @param types
	 * @return
	 */
	public List<Integer> getBuffsByTypes(Collection<Integer> types) {
		List<Integer> buffIds = Lists.newArrayListWithExpectedSize(1);
		final ConfigBuffIndex configBuffIndex = ContentProvider.getConfigIndexProvider().getConfigIndex(IConfigIndexProvider.CONFIG_BUFF);

		// 单个buff生效map  buff类型:标记index:DataPair<value, ConfigBuffBaseInfo.id>
		Map<Integer, Map<Integer, DataPair<Double, Integer>>> typeMarkIndexValueMap = new HashMap<>(1);

		for (BuffInfo buffInfo : getAllBuff()) {
			ConfigBuffBaseInfo buffBaseInfo = configBuffIndex.getBuff(buffInfo.getConfigId());
			if (buffBaseInfo == null){
				continue;
			}

			int buffType = buffBaseInfo.getBuffType().intValue();
			if (types.contains(buffType)) {
				if (!checkNeedMerge(buffBaseInfo.getMarkSingleEffect())) {
					buffIds.add(buffBaseInfo.getId().intValue());

				} else {
					Map<Integer, DataPair<Double, Integer>> markIndexValueMap = typeMarkIndexValueMap.computeIfAbsent(buffType, k -> new HashMap<>());
					putMarkIndexValueMap(markIndexValueMap, buffBaseInfo);
				}

			}
		}

		// 汇总加成buff列表
		typeMarkIndexValueMap.forEach((buffType, markIndexValueMap) -> buffIds.addAll(markIndexValueMap.values().stream().map(o -> o.getObj2()).collect(Collectors.toList())));

		return buffIds;
	}

	/**
	 * 校验是否为标记单个buff生效
	 * @param markSingleEffect markSingleEffect
	 * @return boolean
	 * @date 2024/6/29 16:08
	 */
	private boolean checkNeedMerge(BigInteger markSingleEffect) {
		return markSingleEffect != null && markSingleEffect.intValue() != 0;
	}

	/**
	 * 赋值markIndexValueMap
	 * @param markIndexValueMap 单个buff生效map  标记index:DataPair<value, ConfigBuffBaseInfo.id>
	 * @param buffBaseInfo buffBaseInfo 
	 * @return void
	 * @date 2024/6/29 16:08
	 */
	private void putMarkIndexValueMap(Map<Integer, DataPair<Double, Integer>> markIndexValueMap, ConfigBuffBaseInfo buffBaseInfo) {
		int id = buffBaseInfo.getId().intValue();
		double value = getCompareValue(buffBaseInfo);

		markIndexValueMap.compute(buffBaseInfo.getMarkSingleEffect().intValue(), (markIndex, dataPair) -> {
			if (dataPair == null) {
				return new DataPair<>(value, id);
			} else {
				if (value > dataPair.getObj1()) {
					dataPair.setObj1(value);
					dataPair.setObj2(id);
				}
				return dataPair;
			}
		});
	}

	/**
	 * 获取被比较值
	 * @param buffBaseInfo buffBaseInfo
	 * @return double
	 * @date 2024/6/29 16:08
	 */
	private double getCompareValue(ConfigBuffBaseInfo buffBaseInfo) {
		BigInteger value;

		int mergeFieldType = buffBaseInfo.getMarkValueField().intValue();
		// 比较的字段名: 2: value;
		if (mergeFieldType == BuffConstants.MARK_VALUE_VALUE_FIELD) {
			value = buffBaseInfo.getValue();

			// 比较的字段名: 3: values;
		} else if (mergeFieldType == BuffConstants.MARK_VALUE_VALUES_FIELD) {
			value = buffBaseInfo.getValues().isEmpty() ? BigInteger.ZERO : buffBaseInfo.getValues().get(0);

			// 比较的字段名: 4: values2
		} else if (mergeFieldType == BuffConstants.MARK_VALUE_VALUES2_FIELD) {
			value = buffBaseInfo.getValues2().isEmpty() ? BigInteger.ZERO : buffBaseInfo.getValues2().get(0);

			// 默认比较的字段名: 1: percent;
		} else {
			value = buffBaseInfo.getPercent();
		}

		return value.doubleValue();
	}

	/**
	 * 获取一批buffId 对应是的有效期
	 * @param configIds
	 * @return
	 */
	public Map<Integer, Long> getBuffTimeInfos(Collection<Integer> configIds) {
		if (configIds == null || configIds.isEmpty()) {
			return Maps.newHashMapWithExpectedSize(1);
		}
		Map<Integer, Long> buffTimeInfos = Maps.newHashMapWithExpectedSize(configIds.size());
		for (int configId : configIds) {
			BuffInfo buffInfo = this.data.getSubMapItem(ModelConstant.DATA_KEY_BUFF, configId);
			if (buffInfo == null) {
				continue;
			}
			buffTimeInfos.put(configId, buffInfo.getCreateTime() + buffInfo.getDuration());
		}
		return buffTimeInfos;
	}

	public PlayerBuffCardInfo getPlayerBuffCardInfo() {
		return this.data.get(ModelConstant.DATA_KEY_BUFF_CARD_INFO);
	}

	private void initPlayerBuffCardInfo() {
		PlayerBuffCardInfo info = new PlayerBuffCardInfo();
		this.data.set(ModelConstant.DATA_KEY_BUFF_CARD_INFO, info);
	}
}
