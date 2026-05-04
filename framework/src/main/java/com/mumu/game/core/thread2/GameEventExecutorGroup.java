package com.mumu.game.core.thread2;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.utils.ThreadPoolUtil;

import io.netty.util.concurrent.AbstractEventExecutorGroup;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.RejectedExecutionHandler;
import io.netty.util.concurrent.ScheduledFuture;
import io.netty.util.concurrent.SingleThreadEventExecutor;
import io.netty.util.internal.SystemPropertyUtil;

/**
 * GameEventExecutorGroup
 * 游戏事件执行器组
 * 基于Netty的事件执行器组实现的轻量级线程池管理器，用于游戏服务器中的并发任务处理。
 * 支持通过选择键（selectKey）将相关任务路由到同一个执行器线程，保证任务的顺序执行
 * 主要特性：
 *   <li>管理多个EventExecutor（单线程执行器）</li>
 *   <li>支持轮询和基于键的选择两种任务分发策略</li>
 *   <li>当线程数为2的幂次时使用位运算优化性能</li>
 *   <li>支持优雅关闭和终止状态监控</li>
 * @author liuzhen
 * @version 1.0.0 2026/5/4 16:57
 */
public class GameEventExecutorGroup extends AbstractEventExecutorGroup {
    /** 子执行器数组，每个执行器运行在独立线程上 */
    private final EventExecutor[] children;

    /** 原子计数器，用于轮询方式选择下一个执行器 */
    private final AtomicInteger childIndex = new AtomicInteger();

    /** 原子计数器，跟踪已终止的子执行器数量 */
    private final AtomicInteger terminatedChildren = new AtomicInteger();

    /** 终止未来对象，用于监控所有子执行器的终止状态 */
    @SuppressWarnings("rawtypes")
    private final Promise<?> terminationFuture = new DefaultPromise(GlobalEventExecutor.INSTANCE);

    /**  */
    static final int DEFAULT_MAX_CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
    /** 默认最大待处理任务数，可通过系统属性 io.netty.eventexecutor.maxPendingTasks 配置 */
    static final int DEFAULT_MAX_PENDING_EXECUTOR_TASKS =
            Math.max(16, SystemPropertyUtil.getInt("io.netty.eventexecutor.maxPendingTasks", Integer.MAX_VALUE));


    /**
     * 构造函数，使用指定的线程工厂创建执行器组
     * @param threadNamePrefix 线程名称前缀
     */
    public GameEventExecutorGroup(String threadNamePrefix) {
        this(threadNamePrefix, DEFAULT_MAX_CORE_POOL_SIZE);
    }

    /**
     * 构造函数，使用指定的线程工厂创建执行器组
     * @param threadNamePrefix 线程名称前缀
     * @param playerCorePoolSize 线程数量  = Runtime.getRuntime().availableProcessors() * 2;
     */
    public GameEventExecutorGroup(String threadNamePrefix, int playerCorePoolSize) {
        this(threadNamePrefix, playerCorePoolSize, DEFAULT_MAX_PENDING_EXECUTOR_TASKS);
    }

    /**
     * 构造函数，使用完整参数创建执行器组
     * @param playerCorePoolSize 线程数量
     * @param playerMaxQueueSize 最大待处理任务数，超过此数量的新任务将被拒绝
     */
    public GameEventExecutorGroup(String threadNamePrefix, int playerCorePoolSize, int playerMaxQueueSize) {
        if (playerCorePoolSize <= 0) {
            throw new IllegalArgumentException(String.format("playerCorePoolSize: %d (expected: > 0)", playerCorePoolSize));
        }
        
        // 线程工厂，如果为null则使用默认线程工厂
        ThreadFactory threadFactory = null;
        // 拒绝执行处理器，当任务队列满时如何处理新任务
        RejectedExecutionHandler rejectedHandler;

        children = new SingleThreadEventExecutor[playerCorePoolSize];
        for (int i = 0; i < playerCorePoolSize; i++) {
            boolean success = false;
            try {
                String threadName = threadNamePrefix + i;
                threadFactory = ThreadPoolUtil.newThreadFactory(threadName);
                rejectedHandler = newLogRejectedHandler(threadName);
                children[i] = newChild(threadFactory, playerMaxQueueSize, rejectedHandler);
                success = true;
            } catch (Exception e) {
                // TODO: 思考这是否是一个合适的异常类型
                throw new IllegalStateException("failed to create a child event loop", e);
            } finally {
                if (!success) {
                    // 如果创建失败，关闭之前已成功创建的所有执行器
                    for (int j = 0; j < i; j++) {
                        children[j].shutdownGracefully();
                    }

                    // 等待所有已创建的执行器完全终止
                    for (int j = 0; j < i; j++) {
                        EventExecutor e = children[j];
                        try {
                            while (!e.isTerminated()) {
                                e.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
                            }
                        } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        }

        // 创建终止监听器，当所有子执行器都终止时设置终止未来对象为成功
        final FutureListener<Object> terminationListener = new FutureListener<Object>() {
            @Override
            public void operationComplete(Future<Object> future) throws Exception {
                if (terminatedChildren.incrementAndGet() == children.length) {
                    terminationFuture.setSuccess(null);
                }
            }
        };

        // 为每个子执行器添加终止监听器
        for (EventExecutor e : children) {
            e.terminationFuture().addListener(terminationListener);
        }
    }

    /** 创建拒绝策略（打日志） */
    private static RejectedExecutionHandler newLogRejectedHandler(String threadName) {
        return (r, c) -> LogTopic.ACTION.error("newLogRejectedHandler", threadName,
                " execute fail, queue is full");
    }

    /**
     * 创建新的EventExecutor实例，该方法会被每个服务于本执行器组的线程调用
     * @param threadFactory 线程工厂
     * @param playerMaxQueueSize 最大待处理任务数
     * @param rejectedHandler 拒绝执行处理器
     * @return 新创建的EventExecutor实例
     */
    private EventExecutor newChild(ThreadFactory threadFactory, int playerMaxQueueSize,
                                     RejectedExecutionHandler rejectedHandler) {
        return new DefaultEventExecutor(this, threadFactory, playerMaxQueueSize, rejectedHandler);
    }

    /**
     * 根据选择键选择对应的执行器
     * 相同的selectKey总是会返回同一个执行器，这对于保证同一玩家或同一会话的任务顺序执行非常有用。
     * @param selectKey 选择键，不能为null
     * @return 对应的EventExecutor实例
     * @throws IllegalArgumentException 当selectKey为null时抛出
     */
    private EventExecutor select(Object selectKey) {
        if (selectKey == null) {
            throw new IllegalArgumentException("selectKey不能为空");
        }
        int hashCode = selectKey.hashCode();
        return this.getEventExecutor(hashCode);
    }

    /**
     * 根据给定的值获取对应的执行器
     * 如果执行器数量是2的幂次，则使用位运算优化；否则使用取模运算。
     * @param value 用于选择的整数值
     * @return 对应的EventExecutor实例
     */
    private EventExecutor getEventExecutor(int value) {
        if (isPowerOfTwo(this.children.length)) {
            // 使用位运算优化（相当于取模，但更快）
            return children[value & children.length - 1];
        } else {
            // 使用取模运算
            return children[Math.abs(value % children.length)];
        }
    }

    /**
     * 判断一个数是否是2的幂次
     * @param val 待判断的数值
     * @return true表示是2的幂次，false表示不是
     */
    private static boolean isPowerOfTwo(int val) {
        return (val & -val) == val;
    }

    /**
     * 获取下一个执行器（轮询方式）
     * @return 下一个EventExecutor实例
     */
    @Override
    public EventExecutor next() {
        return this.getEventExecutor(childIndex.getAndIncrement());
    }

    /**
     * 提交一个带返回值的任务到指定执行器
     * @param selectKey 选择键，用于确定目标执行器
     * @param task 要执行的任务
     * @param <T> 返回值类型
     * @return 表示任务执行结果的Future对象
     */
    public <T> Future<T> submit(Object selectKey, Callable<T> task) {
        return this.select(selectKey).submit(task);
    }

    /**
     * 提交一个Runnable任务到指定执行器
     * @param selectKey 选择键，用于确定目标执行器
     * @param task 要执行的任务
     * @return 表示任务执行状态的Future对象
     */
    public Future<?> submit(Object selectKey, Runnable task) {
        return this.select(selectKey).submit(task);
    }

    /**
     * 执行一个命令到指定执行器
     * @param selectKey 选择键，用于确定目标执行器
     * @param command 要执行的命令
     */
    public void execute(Object selectKey, Runnable command) {
        this.select(selectKey).execute(command);
    }

    /**
     * 调度一个带返回值的延迟任务到指定执行器
     * @param selectKey 选择键，用于确定目标执行器
     * @param callable 要执行的任务
     * @param delay 延迟时间
     * @param unit 时间单位
     * @param <V> 返回值类型
     * @return 表示调度任务的ScheduledFuture对象
     */
    public <V> ScheduledFuture<V> schedule(Object selectKey, Callable<V> callable, long delay, TimeUnit unit) {
        return this.select(selectKey).schedule(callable, delay, unit);
    }

    /**
     * 调度一个延迟任务到指定执行器
     * @param selectKey 选择键，用于确定目标执行器
     * @param command 要执行的任务
     * @param delay 延迟时间
     * @param unit 时间单位
     * @return 表示调度任务的ScheduledFuture对象
     */
    public ScheduledFuture<?> schedule(Object selectKey, Runnable command, long delay, TimeUnit unit) {
        return this.select(selectKey).schedule(command, delay, unit);
    }

    /**
     * 调度一个固定频率的周期性任务到指定执行器
     * @param selectKey 选择键，用于确定目标执行器
     * @param command 要执行的任务
     * @param initialDelay 初始延迟时间
     * @param period 周期时间
     * @param unit 时间单位
     * @return 表示调度任务的ScheduledFuture对象
     */
    public ScheduledFuture<?> scheduleAtFixedRate(Object selectKey, Runnable command, long initialDelay, long period,
                                                  TimeUnit unit) {
        return this.select(selectKey).scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    /**
     * 调度一个固定延迟的周期性任务到指定执行器
     * @param selectKey 选择键，用于确定目标执行器
     * @param command 要执行的任务
     * @param initialDelay 初始延迟时间
     * @param delay 每次执行后的延迟时间
     * @param unit 时间单位
     * @return 表示调度任务的ScheduledFuture对象
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(Object selectKey, Runnable command, long initialDelay, long delay,
                                                     TimeUnit unit) {
        return this.select(selectKey).scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    /**
     * 获取所有子执行器的迭代器
     * @return 子执行器迭代器
     */
    @Override
    public Iterator<EventExecutor> iterator() {
        return children().iterator();
    }

    /**
     * 返回所有子执行器的安全副本集合
     * @return 包含所有子执行器的Set集合
     */
    protected Set<EventExecutor> children() {
        Set<EventExecutor> children = Collections.newSetFromMap(new LinkedHashMap<EventExecutor, Boolean>());
        Collections.addAll(children, this.children);
        return children;
    }

    /**
     * 返回本实现使用的EventExecutor数量，该数量与使用的线程数一一对应
     * @return 执行器数量
     */
    public final int executorCount() {
        return children.length;
    }

    /**
     * 优雅关闭所有子执行器
     * @param quietPeriod 静默期，在此期间内没有新任务提交才真正关闭
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 终止未来对象
     */
    @Override
    public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
        for (EventExecutor l : children) {
            l.shutdownGracefully(quietPeriod, timeout, unit);
        }
        return terminationFuture();
    }

    /**
     * 获取终止未来对象，用于监控所有子执行器的终止状态
     * @return 终止未来对象
     */
    @Override
    public Future<?> terminationFuture() {
        return terminationFuture;
    }

    /**
     * 检查是否正在关闭中（任何一个子执行器未处于关闭中状态则返回false）
     * @return true表示正在关闭中，false表示未关闭
     */
    @Override
    public boolean isShuttingDown() {
        for (EventExecutor l : children) {
            if (!l.isShuttingDown()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查是否已关闭（任何一个子执行器未关闭则返回false）
     * @return true表示已关闭，false表示未关闭
     */
    @Override
    public boolean isShutdown() {
        for (EventExecutor l : children) {
            if (!l.isShutdown()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查是否已终止（任何一个子执行器未终止则返回false）
     * @return true表示已终止，false表示未终止
     */
    @Override
    public boolean isTerminated() {
        for (EventExecutor l : children) {
            if (!l.isTerminated()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 等待所有子执行器终止
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return true表示所有执行器已终止，false表示超时
     * @throws InterruptedException 如果等待过程中被中断
     */
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        loop:
        for (EventExecutor l : children) {
            for (;;) {
                long timeLeft = deadline - System.nanoTime();
                if (timeLeft <= 0) {
                    break loop;
                }
                if (l.awaitTermination(timeLeft, TimeUnit.NANOSECONDS)) {
                    break;
                }
            }
        }
        return isTerminated();
    }

    /**
     * 关闭执行器组（委托给shutdownGracefully方法）
     */
    @Override
    public void shutdown() {
        this.shutdownGracefully();
    }
}