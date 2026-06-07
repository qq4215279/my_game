package com.mumu.game.template.component.enums;

import com.mumu.game.template.component.ComponentManager;
import com.mumu.game.template.component.core.IComponent;
import lombok.Getter;

/**
 * ComponentTypeEnum
 * 组件类型枚举
 * @author liuzhen
 * @version 1.0.0 2026/6/7 16:18
 */
@Getter
public enum ComponentTypeEnum {
    /** 任务 */
    TASK(0),
    /** 商城 */
    SHOP(1),
    /** 进度奖励 */
    PROCESS_REWARD(2),
    /** 排行榜 */
    RANK(3),


    ;

    /** 组件类型 */
    private final int activityId;

    ComponentTypeEnum(int activityId) {
        this.activityId = activityId;
    }

    /**
     * getActivity
     * @since 2025/3/21 18:40
     */
    public <T extends IComponent> T getActivity() {
        return ComponentManager.getActivity(activityId);
    }
}
