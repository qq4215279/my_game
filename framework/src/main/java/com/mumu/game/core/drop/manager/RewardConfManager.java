package com.mumu.game.core.drop.manager;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mumu.game.business.item.luban.ItemConfigManager;
import com.mumu.game.constants.Symbol;
import com.mumu.game.core.autoinit.AutoInitEvent;
import com.mumu.game.core.autoinit.AutoInitManager;
import com.mumu.game.core.autoinit.enums.AutoInitModule;
import com.mumu.game.core.drop.anno.DropMapping;
import com.mumu.game.core.drop.consts.DropType;
import com.mumu.game.core.drop.item.DropItem;
import com.mumu.game.core.drop.item.impl.DropItemPack;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.utils.CovertUtil;
import com.mumu.game.core.utils.ModifierUtil;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ReflectUtil;
import org.springframework.stereotype.Component;

/**
 * RewardConfManager
 * 道具奖励配置管理
 * @author liuzhen
 * @version 1.0.0 2026/5/25 20:02
 */
@Component
public class RewardConfManager implements AutoInitEvent {
    private static final LogTopic log = LogTopic.ACTION;

    /** 奖励构造器 Key:itemId, Val:Constructor */
    private static volatile Map<DropType, Constructor<? extends DropItem>> rewardTypeMap = Collections.emptyMap();

    @Override
    public void autoInit() {
        loadRewardType();
    }

    @Override
    public AutoInitModule getInitGroup() {
        return AutoInitModule.LUBAN_DEFALUT_CONFIG;
    }
    

    /** 加载奖励类型掉落解释器 */
    @SuppressWarnings("unchecked")
    private void loadRewardType() {
        try {
            // 过滤有实现的奖励映射
            Set<DropType> filterRewardTypeSet = Sets.newHashSet(DropType.values());
            // 移除NONE
            filterRewardTypeSet.remove(DropType.NONE);

            Map<DropType, Constructor<? extends DropItem>> tmpRewardTypeMap = Maps.newHashMap();
            for (Class<?> clazz : AutoInitManager.CLASSES) {
                DropMapping anno = clazz.getAnnotation(DropMapping.class);
                if (anno == null) {
                    continue;
                }

                Assert.isTrue(ModifierUtil.isBelongTo(clazz, DropItem.class), "道具解释器未继承Reward类: {}", clazz);

                for (DropType type : anno.value()) {
                    Assert.isFalse(tmpRewardTypeMap.containsKey(type), "检测到有重复的道具解释器: {}", type);
                    Constructor<? extends DropItem> constructor = (Constructor<? extends DropItem>) ReflectUtil.getConstructor(clazz, int.class);
                    tmpRewardTypeMap.put(type, constructor);

                    // 初始化remove
                    filterRewardTypeSet.remove(type);
                }
            }

            // 默认奖励构造器
            Class<?> defaultClazz = DropItemPack.class;
            for (DropType type : filterRewardTypeSet) {
                Assert.isFalse(tmpRewardTypeMap.containsKey(type), "检测到有重复的道具解释器: {}", type);
                Constructor<? extends DropItem> constructor = (Constructor<? extends DropItem>) ReflectUtil.getConstructor(defaultClazz, int.class);
                tmpRewardTypeMap.put(type, constructor);
            }

            rewardTypeMap = tmpRewardTypeMap;
            log.info("加载道具奖励解析实现类完成！size: {}", rewardTypeMap.size());
        } catch (Exception e) {
            throw new RuntimeException("加载道具奖励实现类异常", e);
        }
    }

    /** 检查奖励串是否正确 */
    public static boolean checkReward(String rewardStr) {
        return CovertUtil.stringToList(rewardStr, Symbol.SEMICOLON).stream()
                .map(RewardConfManager::createSingleReward)
                .anyMatch(Objects::isNull);
    }

    /** 创建多个奖励解析对象 */
    public static <T extends DropItem> List<T> createRewards(String rewardStr) {
        return createRewards(rewardStr, r -> true);
    }

    /** 创建多个奖励解析对象 */
    public static <T extends DropItem> List<T> createRewards(String rewardStr, Predicate<T> filter) {
        return CovertUtil.stringToList(rewardStr, Symbol.SEMICOLON).stream()
                .map(RewardConfManager::<T>createSingleReward)
                .filter(Objects::nonNull)
                .filter(filter)
                .collect(Collectors.toList());
    }

    /** 创建单个奖励解析对象 */
    @SuppressWarnings("unchecked")
    public static <T extends DropItem> T createSingleReward(String rewardStr) {
        try {
            if (StringUtils.isBlank(rewardStr)) {
                return null;
            }
            String[] rewardFieldArr = rewardStr.split(Symbol.COMMA);
            Assert.isTrue(rewardFieldArr.length > 1, "道具表达式不合法");
            T reward = createSingleReward(Integer.parseInt(rewardFieldArr[0]));
            if (reward != null && reward.parseReward(rewardFieldArr)) {
                return reward;
            }
        } catch (Exception e) {
            log.error(e, "createSingleReward", "rewardStr", rewardStr);
            return null;
        }
        return null;
    }

    /** 创建单个奖励解析对象 */
    @SuppressWarnings("unchecked")
    public static <T extends DropItem> T createSingleReward(int itemId) {
        try {
            Constructor<? extends DropItem> rewardClass = getRewardConstructor(itemId);
            if (rewardClass == null) {
                log.error("createSingleReward", "constructor为空", "itemId", itemId);
                return null;
            }
            return (T) rewardClass.newInstance(new Object[] {itemId});
        } catch (Exception e) {
            log.error(e, "createSingleReward", "itemId", itemId);
            return null;
        }
    }

    /** 获取道具ID对应的奖励解析器 */
    private static Constructor<? extends DropItem> getRewardConstructor(int itemId) {
        DropType type = ItemConfigManager.getRewardType(itemId);
        if (type == null) {
            return null;
        }
        return getRewardConstructor(type);
    }

    /** 获取奖励解析构造器 */
    public static Constructor<? extends DropItem> getRewardConstructor(DropType type) {
        return rewardTypeMap.get(type);
    }
}
