package com.mumu.game.business.function.domain;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.mumu.game.constants.Symbol;
import com.mumu.game.core.log.LogTopic;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * PlayerTemplate
 *
 * @author liuzhen
 * @version 1.0.0 2026/6/7 15:33
 */
@Data
public class PlayerTemplate {
    /** 玩家id */
    private long playerId;
    /** 赛季id */
    private int functionId;
    /** 玩家id */
    private int activityId;
    /** 上次每日重置时间 */
    private long lastResetTime;
    /** 是否赛季结算过 0：否；1已结算 */
    private long seasonCaclState;
    /** 参数。注：只用于记录简单数据，复杂信息走协议！ */
    private String param = StringUtils.EMPTY;

    /** param json对象 */
    private transient JSONObject jsonObject = new JSONObject();

    public void marshal() {
        try{
            if (StringUtils.isNotEmpty(this.param)) {
                JSONObject tmpJsonObject = JSON.parseObject(this.param);
                if (tmpJsonObject != null) {
                    this.jsonObject = tmpJsonObject;
                }
            }
        } catch (Exception e) {
            LogTopic.MODEL.error(e, "PlayerTemplateDO.marshal", "param", param, "templateDO", this);
        }
    }

    public void unmarshal() {
        this.param = this.jsonObject.toJSONString();
    }


    public Object getPrimaryKey() {
        return playerId + Symbol.UNDERLINE + functionId;
    }

    public long getDataId() {
        return playerId;
    }

    /**
     * 每日/每周/每月重置
     * @param resetTime 重置时间
     * @since 2024/12/4 18:13
     */
    public void dailyReset(long resetTime) {
        this.lastResetTime = resetTime;
    }

    /**
     * 周期重置数据
     * @since 2024/12/4 18:13
     */
    public void periodReset() {
        this.activityId = 0;
        this.lastResetTime = 0;
        this.seasonCaclState = 0;
        this.param = StringUtils.EMPTY;
        this.jsonObject = new JSONObject();
        this.markRedPoint();
    }


    // =================================== jsonObject api ===================================
    // int值:
    public int getInt(String key) {
        return this.jsonObject.getIntValue(key, 0);
    }

    public int getInt1() {
        return getInt("int1");
    }

    public int getInt2() {
        return getInt("int2");
    }

    public void setInt1(int intValue) {
        appendParamValue("int1", intValue);
    }

    public void setInt2(int intValue) {
        appendParamValue("int2", intValue);
    }

    // long值:
    public long getLong(String key) {
        return this.jsonObject.getLongValue(key, 0L);
    }

    public long getLong1() {
        return getLong("long1");
    }

    public long getLong2() {
        return getLong("long2");
    }

    public long getLong3() {
        return getLong("long3");
    }

    public void setLong1(long longValue) {
        appendParamValue("long1", longValue);
    }

    public void setLong2(long longValue) {
        appendParamValue("long2", longValue);
    }
    public void setLong3(long longValue) {
        appendParamValue("long3", longValue);
    }

    public void setStr1(String v) {
        appendParamValue("str1", v);
    }

    public void setStr2(String v) {
        appendParamValue("str2", v);
    }

    public String getStr1() {
        return getStr("str1");
    }

    public String getStr2() {
        return getStr("str2");
    }

    public String getStr(String key) {
        return this.jsonObject.getString(key);
    }

    // boolean值:
    public boolean getBoolean(String key) {
        return this.jsonObject.getBooleanValue(key, false);
    }

    public boolean getBoolean1() {
        return getBoolean("boolean1");
    }

    public boolean getBoolean2() {
        return getBoolean("boolean2");
    }

    public void setBoolean1(boolean booleanValue) {
        appendParamValue("boolean1", booleanValue);
    }

    public void setBoolean2(boolean booleanValue) {
        appendParamValue("boolean2", booleanValue);
    }

    /**
     * 活动是否有红点
     * 作用：1. 周期活动开启首次红点; 2. 每日红点
     * @return boolean true: 有红点; false: 标识已清除红点
     * @since 2025/5/29 10:43
     */
    public boolean hasRedPoint() {
        return getBoolean("redPoint");
    }

    /**
     * 标记红点
     * @since 2025/7/14 11:12
     */
    public void markRedPoint() {
        appendParamValue("redPoint", true);
    }

    /**
     * 清除活动红点
     * @since 2025/5/29 10:28
     */
    public void cleanRedPoint() {
        appendParamValue("redPoint", false);
    }

    /** 获取或创建 JSONArray (指针引用,可直接修改 JSONArray中元素) */
    public JSONArray getList(String key) {
        JSONArray jsonArray = jsonObject.getJSONArray(key);
        if (jsonArray == null) {
            appendParamValue(key, jsonArray = new JSONArray());
        }

        return jsonArray;
    }

    public JSONArray getList1() {
        return getList("list1");
    }

    public JSONArray getList2() {
        return getList("list2");
    }

    /** 获取或创建 JSONArray (指针引用,可直接修改 JSONArray中元素) */
    public JSONObject getMap(String key) {
        JSONObject subJsonObject = jsonObject.getJSONObject(key);
        if (subJsonObject == null) {
            appendParamValue(key, subJsonObject = new JSONObject());
        }

        return subJsonObject;
    }

    public JSONObject getMap1() {
        return getMap("map1");
    }

    public JSONObject getMap2() {
        return getMap("map2");
    }

    /**
     * 添加参数值
     * @param key key
     * @param value value
     * @since 2025/5/12 17:26
     */
    public void appendParamValue(String key, Object value) {
        this.jsonObject.put(key, value);
    }

    // =================================== jsonObject api ===================================
}
