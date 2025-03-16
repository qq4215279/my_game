package com.mumu.common.util2;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Pair;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mumu.common.constants.SymbolConstants;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;

/** 位运算工具类，可以用于标记奖励领取状态 @Date: 2024/10/29 上午11:28 @Author: xu.hai */
public final class Bit64Util {
  public static final int RANGE = Long.SIZE - 1;

  /** 检查指定bit的值是否为1 */
  public static boolean checkBitT(long state, int pos) {
    if (pos < 1 || pos > RANGE) {
      throw new IllegalArgumentException("参数错误,pos的范围只能是1到63之间,当前传的pos为:" + pos);
    }
    return (state & ((long) 1 << pos - 1)) != 0;
  }

  /** 设置指定bit的值为1 */
  public static long setBitT(long state, int pos) {
    if (pos < 1 || pos > RANGE) {
      throw new IllegalArgumentException("参数错误,pos的范围只能是1到63之间,当前传的pos为:" + pos);
    }
    return state | ((long) 1 << (pos - 1));
  }

  /** 设置指定bit的值为0 */
  public static long setBitF(long state, int pos) {
    if (pos < 1 || pos > RANGE) {
      throw new IllegalArgumentException("参数错误,pos的范围只能是1到63之间,当前传的pos为:" + pos);
    }
    return state & (~((long) 1 << (pos - 1)));
  }

  /**
   * 检查奖励领取状态
   *
   * @param statusList 领取状态集
   * @param bitIndex 状态bit位（从1开始）
   * @return true-已领取
   */
  public static boolean checkBitT(List<Long> statusList, int bitIndex) {
    if (CollUtil.isEmpty(statusList)) return false;

    // 因为从1bit开始，而list下标计算从0开始，所以在此计算时-1
    int index = (bitIndex - 1) / RANGE;
    if (statusList.size() <= index) {
      return false;
    }
    long status = statusList.get(index);
    int pos = bitIndex - index * RANGE;
    return checkBitT(status, pos);
  }

  /**
   * 设置奖励领取状态
   *
   * @param statusList 领取状态集
   * @param bitIndex 状态bit位 （从1开始）
   */
  public static void setBitT(List<Long> statusList, int bitIndex) {
    // 因为从1级开始，而下标计算送0开始，所以在此计算时-1
    int index = (bitIndex - 1) / RANGE;
    if (statusList.size() <= index) {
      for (int i = statusList.size(); i <= index; i++) {
        statusList.add(0L);
      }
    }
    long status = statusList.get(index);
    int pos = bitIndex - index * RANGE;
    status = setBitT(status, pos);
    statusList.set(index, status);
  }

  /** 获取所在分组和组内索引 k-组 v-组内索引，注：id > 0 */
  public static Pair<Integer, Integer> getGIPair(int id) {
    Assert.isTrue(id > 0, "参数错误，id必须大于0：" + id);
    int group = id / RANGE;
    int pos = id % RANGE;
    if (pos == 0) {
      group--;
      pos = RANGE;
    }
    return Pair.of(group, pos);
  }

  /** 设置组状态T k-组 v-组状态 */
  public static void setGIT(Map<Integer, Long> stateMap, int id) {
    if (stateMap == null) return;
    Pair<Integer, Integer> pair = getGIPair(id);
    stateMap.compute(pair.getKey(), (k, v) -> setBitT(v == null ? 0 : v, pair.getValue()));
  }

  /** 设置组状态F k-组 v-组状态 */
  public static void setGIF(Map<Integer, Long> stateMap, int id) {
    if (stateMap == null) return;
    Pair<Integer, Integer> pair = getGIPair(id);
    stateMap.compute(pair.getKey(), (k, v) -> setBitF(v == null ? 0 : v, pair.getValue()));
  }

  /** 检查组状态 k-组 v-组状态 */
  public static boolean checkGI(Map<Integer, Long> stateMap, int id) {
    if (CollUtil.isEmpty(stateMap)) return false;
    Pair<Integer, Integer> pair = getGIPair(id);
    return checkBitT(stateMap.getOrDefault(pair.getKey(), 0L), pair.getValue());
  }

  /**
   * 组状态map转字符串存储
   * <li>连续的组状态用逗号分隔存储，且状态long转为32进制存储字符串存储
   * <li>如：{0=1026, 1=4097, 3=6144, 4=98, 6=134348928} 转为：0:102,401;3:600,32;6:404040
   *
   * @param stateMap key-组 value-组状态
   * @return 字符串 (0:102,401;3:600,32;6:404040)
   */
  public static String giMapToStr(Map<Integer, Long> stateMap) {
    if (CollUtil.isEmpty(stateMap)) return StringUtils.EMPTY;
    StringBuilder sb = new StringBuilder();
    int pre = Integer.MIN_VALUE;
    TreeSet<Integer> keys = Sets.newTreeSet(stateMap.keySet());
    for (int key : keys) {
      String state = Long.toUnsignedString(stateMap.get(key), 32);
      if (key != pre + 1) {
        sb.append(SymbolConstants.SEMICOLON)
            .append(key)
            .append(SymbolConstants.COLON)
            .append(state);
      } else {
        sb.append(SymbolConstants.COMMA).append(state);
      }
      pre = key;
    }
    // 删除第一个分号
    if (sb.indexOf(SymbolConstants.SEMICOLON) == 0) sb.delete(0, 1);
    return sb.toString();
  }

  /** 组状态字符串转为map（字符串格式 0:102,401;3:600,32;6:404040） */
  public static Map<Integer, Long> strTogiMap(String giStateMapStr) {
    Map<Integer, Long> map = Maps.newTreeMap();
    if (StringUtils.isEmpty(giStateMapStr)) return map;
    for (String groupStr : giStateMapStr.split(SymbolConstants.SEMICOLON)) {
      String[] groupArr = groupStr.split(SymbolConstants.COLON);
      if (groupArr.length != 2) continue;
      int group = Integer.parseInt(groupArr[0]);
      for (String stateStr : groupArr[1].split(SymbolConstants.COMMA)) {
        if (StringUtils.isNotEmpty(stateStr))
          map.put(group++, Long.parseUnsignedLong(stateStr, 32));
      }
    }
    return map;
  }

  public static void main(String[] args) {
    testSetState();
    testStateList();
    testGIMap();
  }

  /** 测试修改状态 */
  private static void testSetState() {
    System.out.println("\n============ 测试 testSetState");
    long state = 0;
    for (int i = 1; i < 64; i++) {
      state = setBitT(state, i);
      System.out.print(state + " ");
    }
  }

  /** 测试状态List */
  private static void testStateList() {
    System.out.println("\n\n============ 测试 testStateList");
    List<Long> statusList = Lists.newArrayList();
    // 设置指定等级状态为1，模拟奖励已领取
    List<Integer> levels = List.of(1, 50, 63, 64, 127, 128);
    for (int level : levels) {
      setBitT(statusList, level);
    }
    System.out.println("领奖状态: " + statusList);

    List<Integer> checkLevels = List.of(1, 20, 50, 128);
    for (int level : checkLevels) {
      System.out.printf("等级%d, 奖励是否领取：%b %n", level, checkBitT(statusList, level));
    }
  }

  /** 测试状态组 */
  private static void testGIMap() {
    System.out.println("\n\n============ 测试 testGIMap");
    Map<Integer, Long> stateMap = Maps.newHashMap();
    for (int i : Arrays.asList(1, 62, 63, 64, 126, 127, 500)) {
      setGIT(stateMap, i);
      System.out.println(i + ":" + checkGI(stateMap, i));
    }
    System.out.println("125:" + checkGI(stateMap, 125));
    System.out.println(stateMap);

    String str = giMapToStr(stateMap);
    System.out.println("giMapToStr: " + str);
    System.out.println("strTogiMap: " + strTogiMap(str));
  }
}
