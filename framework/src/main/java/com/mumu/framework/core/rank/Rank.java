package com.mumu.framework.core.rank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Rank
 * 排行榜
 * 注：排行榜提供所有玩家实时排名，但只有部分是精确排名，其余为非精确排名
 * @author liuzhen
 * @version 1.0.0 2025/3/15 16:30
 */
public class Rank {
  /** 最大准确数量 默认10000 */
  final int maxNum;
  /** 积分区间大小 */
  final int pointInterval;

  /** 精确排名列表   */
  final List<RankData> rankList;

  /** 非精确排名  */
  private final List<Integer> extraRankList;

  /** 内部锁  */
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final Lock readLock = lock.readLock();
  private final Lock writeLock = lock.writeLock();

  /**
   * 构造函数
   * @param maxNum maxNum
   * @param pointInterval pointInterval
   * @date 2025/3/15 17:56
   */
  Rank(int maxNum, int pointInterval) {
    this.maxNum = maxNum;
    this.pointInterval = pointInterval;

    this.rankList = new ArrayList<>(this.maxNum);
    this.extraRankList = new ArrayList<>();
  }

  /**
   * 排行榜初始化
   * @param dataList dataList
   * @date 2025/3/15 17:54
   */
  void init(List<RankData> dataList) {
    for (RankData data : dataList) {
      // 1. 精确排名范围内
      if (rankList.size() < maxNum) {
        rankList.add(data);

        // 2. 非精确排名
      } else {
        int index = (int)(data.score / pointInterval);
        rangeCheck(index, extraRankList);
        extraRankList.set(index, extraRankList.get(index) + 1);
      }
    }
  }

  /**
   * 获取最大精确排名
   * @return int
   * @date 2025/3/15 17:54
   */
  int getMaxNum() {
    return rankList.size();
  }

  /**
   * 增加对象
   * @param data 排名数据
   * @date 2025/3/15 17:54
   */
  void addRankData(RankData data) {
    writeLock.lock();
    try {
      // 1. 精确排行榜未满
      if (rankList.size() < maxNum) {
        add2RankList(data);
        return;
      }

      // 2. 精确排行榜已满
      RankData last = rankList.get(rankList.size() - 1);
      // 2.1. 小于最后一名分数 => 添加到非精确排行榜
      if (data.score <= last.score) {
        add2ExtraRankList(data);

        // 2. 添加到精确排行榜
      } else {
        add2RankList(data);

        // 移除末尾元素
        removeTail();
      }
    } finally {
      writeLock.unlock();
    }
  }

  /**
   * 更新积分
   * @param data data
   * @param srcValue srcValue
   * @date 2025/3/15 17:59
   */
  void updateRankData(RankData data, int srcValue) {
    updateRankData(data, srcValue, -1L);
  }

  /**
   * 更新积分
   * @param data 排行数据
   * @param srcValue srcValue
   * @param srcTimestamp 上一次更新积分时间
   * @date 2025/3/15 16:47
   */
  void updateRankData(RankData data, long srcValue, long srcTimestamp) {
    writeLock.lock();
    // 历史数据
    RankData srcData = data.clone();
    srcData.score = srcValue;
    if (srcTimestamp != -1L) {
      srcData.timestamp = srcTimestamp;
    }

    if (rankList.isEmpty()) {
      return;
    }

    try {
      // 1. 精确排行榜最后一个人
      RankData last = rankList.get(rankList.size() - 1);
      if (data.score < last.score && srcData.score < last.score && rankList.size() >= maxNum) {
        // 在非精确排行榜
        addAndRemoveExtraRankList(data, srcData);

        // 2. 进入非精确榜单
      } else {
        // 从准确排行榜中查找自己
        int myIndex = Collections.binarySearch(rankList, srcData);
        // 2.1. 在精确排行榜内
        if (myIndex >= 0) {
          // 删除自己
          rankList.remove(myIndex);
          add2RankList(data);

        } else {
          myIndex = Collections.binarySearch(rankList, data);
          if (Math.abs(myIndex) <= maxNum) {
            // 升级到了精准排行榜
            add2RankList(data);

            // 从非精准排行榜删除
            int index = (int)(srcData.score / pointInterval);
            if (extraRankList.size() > index) {
              extraRankList.set(index, extraRankList.get(index) - 1);
            }

            // 移除精准排行榜最后一位
            removeTail();
          } else {
            // 如果精准排行榜未满，先插入精准排行榜
            if (rankList.size() < maxNum) {
              add2RankList(data);
            } else {
              addAndRemoveExtraRankList(data, srcData);
            }
          }
        }
      }
    } finally {
      writeLock.unlock();
    }
  }

  /**
   * 移除排行版数据
   * @param data data 
   * @date 2025/3/15 16:47
   */
  void removeRankData(RankData data) {
    writeLock.lock();
    try {
      // 精确排行榜最后一个人
      RankData last = rankList.get(rankList.size() - 1);
      if (data.score < last.score && rankList.size() >= maxNum) {
        int index = (int)(data.score / pointInterval);
        rangeCheck(index, extraRankList);
        extraRankList.set(index, extraRankList.get(index) - 1);
      } else {
        // 从准确排行榜中查找自己
        int myIndex = Collections.binarySearch(rankList, data);
        if (myIndex >= 0) {
          // 删除自己
          rankList.remove(myIndex);
        }
      }
    } finally {
      writeLock.unlock();
    }
  }

  /**
   * 获取排行榜数据
   * @param start start
   * @param end end 
   * @return java.util.List<com.game.framework.core.rank.RankData>
   * @date 2025/3/15 16:49
   */
  List<RankData> getRankList(int start, int end) {
    List<RankData> rtnList = new ArrayList<>();

    // 不能超过目前最大排名
    int max = rankList.size();
    end = Math.min(end, max);

    readLock.lock();
    try {
      for (int i = start; i < end; i++) {
        rtnList.add(rankList.get(i));
      }
    } finally {
      readLock.unlock();
    }
    return rtnList;
  }

  /**
   * 获得自己的排名
   * @param data data
   * @return int
   * @date 2025/3/15 16:49
   */
  int getMyRank(RankData data) {
    // 排行榜中无数据
    if (data == null || rankList.isEmpty()) {
      return -1;
    }

    readLock.lock();
    try {
      // 1. 是否在准确榜
      RankData last = rankList.get(rankList.size() - 1);
      if (data.score >= last.score) {
        int myIndex = Collections.binarySearch(rankList, data);
        if (myIndex >= 0) {
          return myIndex + 1;
        }
      }

      // 2. 在非准确榜
      int index = (int)(data.score / pointInterval);
      // 评估排名
      int beforeNum = rankList.size();
      int lowPoint = (index + 1) * pointInterval;
      // 在非精确排名中，找比之前排名考前的数量
      for (int i = extraRankList.size() - 1; i > index; i--) {
        beforeNum += extraRankList.get(i);
      }

      // 在非精确列表，最大区间
      if (index > extraRankList.size() - 1) {
        return beforeNum + 1;

      } else {
        // 平均值，算出该区间的密度(+1防止除0)
        double avg =  pointInterval * 1.0 / (extraRankList.get(index) + 1);
        beforeNum += (int) Math.ceil((lowPoint - data.score) / avg);
        return beforeNum;
      }

    } finally {
      readLock.unlock();
    }
  }

  /**
   * 添加到精确排名中
   * @param data data
   * @date 2025/3/15 16:49
   */
  private void add2RankList(RankData data) {
    int myIndex = Collections.binarySearch(rankList, data);
    if (myIndex >= 0) {
      rankList.set(myIndex, data);
    } else {
      rankList.add(-myIndex - 1, data);
    }
  }

  /**
   * 插入非准确排行榜
   * @date 2025/3/15 16:50
   */
  private void add2ExtraRankList(RankData data) {
    int index = (int)(data.score / pointInterval);
    rangeCheck(index, extraRankList);
    extraRankList.set(index, extraRankList.get(index) + 1);
  }

  /**
   * 从非准确排行榜移除
   * @param newData newData
   * @param oldData oldData
   * @date 2025/3/15 16:50
   */
  private void addAndRemoveExtraRankList(RankData newData, RankData oldData) {
    int newIndex = (int)(newData.score / pointInterval);
    int oldIndex = (int)(oldData.score / pointInterval);
    if (newIndex != oldIndex) {
      rangeCheck(oldIndex, extraRankList);
      extraRankList.set(oldIndex, extraRankList.get(oldIndex) - 1);
      rangeCheck(newIndex, extraRankList);
      extraRankList.set(newIndex, extraRankList.get(newIndex) + 1);
    }
  }

  /**
   * 删除准确榜最后一名，添加到非准确榜人数
   * @date 2025/3/15 16:50
   */
  private void removeTail() {
    if (rankList.size() < maxNum) {
      return;
    }
    RankData data = rankList.remove(rankList.size() - 1);
    add2ExtraRankList(data);
  }

  /**
   * 范围检查
   * @param index index
   * @param rankList rankList
   * @date 2025/3/15 16:50
   */
  private void rangeCheck(int index, List<Integer> rankList) {
    if (rankList.size() <= index) {
      for (int i = rankList.size(); i <= index; i++) {
        rankList.add(0);
      }
    }
  }


  public static void main(String[] args) {
    List<Integer> list = new ArrayList<>();
    list.add(2);
    list.add(3);
    list.add(6);
    list.add(7);

    // int data = 0;
    // int data = 3;
    int data = 100;
    int myIndex = Collections.binarySearch(list, data);
    System.out.println(myIndex);

    if (myIndex >= 0) {
      list.set(myIndex, data);
    } else {
      list.add(-myIndex - 1, data);
    }
    System.out.println(list);
  }
}
