package com.mumu.game.template.func.domain;

import com.alibaba.fastjson2.JSONObject;
import com.mumu.game.constants.Symbol;
import com.mumu.game.proto.function.SingleFunctionInfoBean;
import lombok.Data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * PlayerTemplateState
 *
 * @author liuzhen
 * @version 1.0.0 2026/6/7 14:39
 */
@Data
public class PlayerTemplateState {

    /** 玩家id */
    private long playerId;
    /** 功能id */
    private int functionId;

    /** 是否开启 */
    private boolean isOpen;
    /** 是否有小红点 */
    private boolean hasRedPoint;
    /** 开放的活动组件ID列表 */
    private List<Integer> activityIds;
    /** 赛季开始时间，无默认-1 */
    private long startTime = -1;
    /** 赛季结束时间，无默认-1 */
    private long endTime = -1;
    /** 客户端参数 */
    private String clientParam;

    /** 记录变更字段 */
    private Set<String> changeFieldNameSet = new HashSet<>();

    /** param json对象 */
    private transient JSONObject jsonObject = new JSONObject();

    public Object getPrimaryKey() {
        return playerId + Symbol.UNDERLINE + functionId;
    }

    public long getDataId() {
        return playerId;
    }

    public void setOpen(boolean isOpen) {
        if (isOpen == this.isOpen) {
            return;
        }
        this.isOpen = isOpen;
        markChange("isOpen");
    }

    public void setHasRedPoint(boolean hasRedPoint) {
        if (hasRedPoint == this.hasRedPoint) {
            return;
        }
        this.hasRedPoint = hasRedPoint;
        markChange("hasRedPoint");
    }

    public void setActivityIds(List<Integer> activityIds) {
        this.activityIds = activityIds;
        markChange("activityIds");
    }

    public void setStartTime(long startTime) {
        if (startTime == this.startTime) {
            return;
        }
        this.startTime = startTime;
        markChange("startTime");
    }

    public void setEndTime(long endTime) {
        if (endTime == this.endTime) {
            return;
        }
        this.endTime = endTime;
        markChange("endTime");
    }

    public void setClientParam(String clientParam) {
        if (clientParam == null || clientParam.equals(this.clientParam)) {
            return;
        }
        this.clientParam = clientParam;
        markChange("clientParam");
    }

    /** 标记变更 */
    private void markChange(String fieldName) {
        changeFieldNameSet.add(fieldName);
    }

    /**
     * state 转 SingleFunctionInfoBean
     * @param buildAll 构建所有
     * @return com.game.proto.function.SingleFunctionInfoBean
     * @since 2025/7/7 11:28
     */
    public SingleFunctionInfoBean convert2Bean(boolean buildAll) {
        SingleFunctionInfoBean bean = new SingleFunctionInfoBean();

        bean.setFunctionId(functionId);

        // 构建所有
        if (buildAll) {
            bean.setHasOpen(this.isOpen);
            bean.setHasRedPoint(this.hasRedPoint);
            bean.setActivityIds(this.activityIds);
            bean.setStartTime(this.startTime);
            bean.setEndTime(this.endTime);
            bean.setParam(this.clientParam);

            //
        } else {
            if (changeFieldNameSet.contains("isOpen")) {
                bean.setHasOpen(this.isOpen);
            }
            // 是否拥有红点（子类有红点，则映射父类有红点）
            if (changeFieldNameSet.contains("hasRedPoint")) {
                bean.setHasRedPoint(this.hasRedPoint);
            }
            if (changeFieldNameSet.contains("activityIds")) {
                bean.setActivityIds(this.activityIds);
            }
            if (changeFieldNameSet.contains("startTime")) {
                bean.setStartTime(this.startTime);
            }
            if (changeFieldNameSet.contains("endTime")) {
                bean.setEndTime(this.endTime);
            }
            if (changeFieldNameSet.contains("clientParam")) {
                bean.setParam(this.clientParam);
            }
        }

        boolean hasChange = buildAll || !this.changeFieldNameSet.isEmpty();
        // 清除标记
        this.changeFieldNameSet.clear();

        return hasChange ? bean : null;
    }


    // =================================== jsonObject api ===================================

    /**
     * 添加参数值
     * @param key key
     * @param value value
     * @since 2025/8/4 15:08
     */
    public void appendParamValue(String key, Object value) {
        this.jsonObject.put(key, value);
    }

    // =================================== jsonObject api ===================================
}
