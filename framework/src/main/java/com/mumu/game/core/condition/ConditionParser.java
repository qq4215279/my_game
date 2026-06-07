package com.mumu.game.core.condition;

/**
 * ConditionParser
 * 条件解析器
 * @author liuzhen
 * @version 1.0.0 2026/6/7 15:46
 */
public class ConditionParser {

    public static ConditionParser of(long playerId, String condition) {
        return new ConditionParser(playerId, condition);
    }

    public ConditionParser(long playerId, String condition) {
    }


    public boolean checkCondition() {
        return true;
    }
}
