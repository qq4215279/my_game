package com.mumu.framework.util2;

/** 随机工具类 @Date: 2025/2/13 下午3:14 @Author: xu.hai */
public class RandomUtils extends cn.hutool.core.util.RandomUtil {

  /** 必中 */
  public static final int SURE_HIT = -1;

  /** 精度-百分比 */
  public static final int SCALE_100 = 100;

  /**
   * 概率命中
   *
   * @param prob 概率值 [0,scale]，特殊的，-1表示必中
   * @param scale 精度范围
   * @return true-命中
   */
  public static boolean hit(int prob, int scale) {
    if (prob == SURE_HIT || prob >= scale) return true;
    return randomInt(scale) < prob;
  }

  /** 概率命中，百分比 */
  public static boolean hit(int prob) {
    return hit(prob, SCALE_100);
  }

  /** 从[min, max]中随机一个值（注，与randomInt的[min, max)随机范围不同） */
  public static int rollInt(int min, int max) {
    return randomInt(Math.min(min, max), Math.max(min, max) + 1);
  }

  /** 从[1, max]中随机一个值（注，与randomInt的[min, max)随机范围不同） */
  public static int rollInt(int max) {
    return max < 1 ? 0 : rollInt(1, max);
  }
}
