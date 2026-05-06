package com.mumu.game.core.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.game.framework.core.consts.CoreConstants;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.NumberUtil;

/**
 * MathUtil 计算工具类
 *
 * @author liuzhen
 * @version 1.0.0 2025/7/2 10:38
 */
public class MathUtil extends NumberUtil {

  /**
   * 计算百分比占比数量
   *
   * @param total 总数
   * @param percent 所占百分比
   * @return long
   * @since 2025/7/2 10:47
   */
  public static long calPercent(long total, int percent) {
    return (long) (total * (double) percent / CoreConstants.GAME_PERCENT_RATE);
  }

  /**
   * 计算在指定index 占比数量
   * @param total 总数
   * @param index 指定index
   * @param percentList percentList
   * @return long
   * @since 2025/8/6 17:32
   */
  public static long calPercent(long total, int index, List<Integer> percentList) {
    if (index < 0 || index > percentList.size() - 1) {
      return 0;
    }

    int percent = percentList.get(index);
    int totalPercent = percentList.stream().mapToInt(Integer::intValue).sum();

    return (long) (total * (double) percent / totalPercent);
  }

  /**
   * 计算万分比占比数量
   *
   * @param total 总数
   * @param percent 所占万分比
   * @return long
   * @since 2025/7/2 10:48
   */
  public static long calPercent10000(long total, int percent) {
    return (long) (total * (double) percent / CoreConstants.GAME_TOTAL_RATE);
  }

  /** 不保留小数的累除 */
  public static BigDecimal div(RoundingMode roundingMode, Number... vs) {
    return div(roundingMode, 0, vs);
  }

  /** 累除 */
  public static BigDecimal div(RoundingMode roundingMode, int scale, Number... vs) {
    return div(
        roundingMode,
        scale,
        ArrayUtil.map(vs, BigDecimal.class, v -> new BigDecimal(v.toString())));
  }

  /** 累除 */
  public static BigDecimal div(RoundingMode roundingMode, int scale, String... vs) {
    return div(roundingMode, scale, ArrayUtil.map(vs, BigDecimal.class, BigDecimal::new));
  }

  /** 累除 */
  public static BigDecimal div(RoundingMode roundingMode, int scale, BigDecimal... vs) {
    if (ArrayUtil.isEmpty(vs) || ArrayUtil.hasNull(vs)) {
      return BigDecimal.ZERO;
    }
    BigDecimal result = vs[0];
    for (int i = 1; i < vs.length; ++i) {
      result = result.divide(vs[i], scale, roundingMode);
    }
    return result;
  }
}
