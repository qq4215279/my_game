// package com.mumu.framework.core.log;
//
// import com.google.common.collect.MinMaxPriorityQueue;
// import java.util.Comparator;
// import java.util.concurrent.TimeUnit;
// import java.util.concurrent.atomic.AtomicBoolean;
// import lombok.Getter;
//
// /**
//  * SlowLogUtil
//  * 统计下最耗时的TopN
//  * @author liuzhen
//  * @version 1.0.0 2025/3/16 15:08
//  */
// public class SlowLogUtil {
//   /** Top10的耗时请求 */
//   static volatile MinMaxPriorityQueue<com.mumu.framework.core.log2.SlowLogUtil.Request> slowActionQueue = getRequests();
//
//   private static MinMaxPriorityQueue<com.mumu.framework.core.log2.SlowLogUtil.Request> getRequests() {
//     return MinMaxPriorityQueue.orderedBy(
//             Comparator.comparingInt(com.mumu.framework.core.log2.SlowLogUtil.Request::getDuration).reversed())
//         .maximumSize(10)
//         .create();
//   }
//
//   static AtomicBoolean running = new AtomicBoolean();
//
//   private static void registerTask() {
//     if (running.compareAndSet(false, true)) {
//       ScheduledExecutorUtil.scheduleWithFixedDelay(
//           "SlowLogUtil", com.mumu.framework.core.log2.SlowLogUtil::printSlowLog, 30, 6000, TimeUnit.SECONDS);
//     }
//   }
//
//   /** 记录请求耗时 */
//   public static void record(String name, int duration) {
//     try {
//       if (LogSwitch.CMD.getBool()) {
//         // slowActionQueue.add(new Request(name, duration));
//         // registerTask();
//       }
//     } catch (Exception e) {
//       LogTopic.NET.error(e, "record SlowLog error");
//     }
//   }
//
//   /** 打印日志 */
//   public static void printSlowLog() {
//     MinMaxPriorityQueue<com.mumu.framework.core.log2.SlowLogUtil.Request> lastQueut = slowActionQueue;
//     slowActionQueue = getRequests();
//
//     StringBuilder sb = new StringBuilder("Top10 slow action \n");
//     int i = 0;
//     for (com.mumu.framework.core.log2.SlowLogUtil.Request request : lastQueut) {
//       try {
//         sb.append(++i)
//             .append(". ")
//             .append(request.getName())
//             .append(" : ")
//             .append(request.getDuration())
//             .append("ns = ")
//             .append(request.getDuration() / 1000000)
//             .append("ms \n");
//       } catch (Exception e) {
//         LogTopic.NET.error(e, "printSlowLog error");
//       }
//     }
//     if (i > 0) LogTopic.NET.debug(LogSwitch.CMD, sb.toString());
//   }
//
//   @Getter
//   static class Request {
//     private final String name;
//     private final int duration;
//
//     public Request(String name, int duration) {
//       this.name = name;
//       this.duration = duration;
//     }
//   }
// }
