/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.timer.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.mumu.game.core.timer.bo.GameTimerContext;
import com.mumu.game.core.timer.bo.GameTimerDefinition;
import com.mumu.game.core.timer.bo.GameTimerTaskSnapshot;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.mumu.game.core.autoinit.AutoInitEvent;
import com.mumu.game.core.autoinit.enums.AutoInitModule;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.net.consts.ServiceType;
import com.mumu.game.core.timer.anno.GameTimer;
import com.mumu.game.core.timer.core.trigger.CronTimerTrigger;
import com.mumu.game.core.timer.core.trigger.DynamicTimerTrigger;
import com.mumu.game.core.timer.core.trigger.FixedDelayTimerTrigger;
import com.mumu.game.core.timer.core.trigger.TimerTrigger;
import com.mumu.game.core.timer.util.CronUtil;

/**
 * GameTimerManager
 * 游戏周期性任务管理器
 * @author liuzhen
 * @version 2.0.0 2026/8/9 12:19
 */
@Component
public class GameTimerManager implements AutoInitEvent {

    /** Spring 容器 */
    private final ApplicationContext applicationContext;
    /** 只负责到期触发的调度线程池 */
    private final ScheduledExecutorService scheduledExecutor;
    /** 负责执行任务方法的服务线程池 */
    private final Executor serverExecutor;
    /** 任务执行拦截器 */
    private final List<GameTimerInterceptor> interceptors;
    /** 本进程已注册的任务 */
    private final Map<String, GameTimerTaskInfo> taskMap = new ConcurrentHashMap<>();
    /** 初始化状态，防止重复扫描注册 */
    private final AtomicBoolean initialized = new AtomicBoolean();
    /** 关闭状态，关闭后禁止继续调度 */
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * 创建游戏周期性任务管理器
     * @param applicationContext Spring容器
     * @param scheduledExecutor 调度线程池
     * @param serverExecutor 服务任务线程池
     */
    @Autowired
    public GameTimerManager(ApplicationContext applicationContext,
        @Qualifier("scheduledExecutor") ScheduledExecutorService scheduledExecutor,
        @Qualifier("serverExecutor") Executor serverExecutor, List<GameTimerInterceptor> interceptors) {
        this.applicationContext = applicationContext;
        this.scheduledExecutor = scheduledExecutor;
        this.serverExecutor = serverExecutor;
        List<GameTimerInterceptor> sortedInterceptors = new ArrayList<>(interceptors);
        sortedInterceptors.sort(Comparator.comparingInt(GameTimerInterceptor::order));
        this.interceptors = List.copyOf(sortedInterceptors);
    }

    /**
     * 创建不带拦截器的游戏周期性任务管理器
     * @param applicationContext Spring容器
     * @param scheduledExecutor 调度线程池
     * @param serverExecutor 服务任务线程池
     */
    public GameTimerManager(ApplicationContext applicationContext, ScheduledExecutorService scheduledExecutor,
        Executor serverExecutor) {
        this(applicationContext, scheduledExecutor, serverExecutor, List.of());
    }

    @Override
    public AutoInitModule getInitGroup() {
        return AutoInitModule.GAME_START;
    }

    /**
     * 优先初始化任务管理器，保证其他 GAME_START 组件可以动态注册任务
     * @return 初始化顺序
     */
    @Override
    public int order() {
        return -1_000;
    }

    @Override
    public void autoInit() {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }

        try {
            Map<String, GameTimerTaskInfo> discoveredTasks = discoverTasks();
            taskMap.putAll(discoveredTasks);
            for (GameTimerTaskInfo taskInfo : discoveredTasks.values()) {
                registerInitialTask(taskInfo);
            }
            LogTopic.ACTION.info("GameTimerManager.init", "taskCount", taskMap.size(), "keys", taskMap.keySet());
        } catch (RuntimeException e) {
            taskMap.values().forEach(GameTimerTaskInfo::stop);
            taskMap.clear();
            initialized.set(false);
            throw e;
        }
    }

    /**
     * 暂停指定任务，正在执行的任务不会被中断
     * @param key 任务唯一标识
     * @return true表示暂停成功
     */
    public boolean pause(String key) {
        GameTimerTaskInfo taskInfo = taskMap.get(key);
        return taskInfo != null && taskInfo.pause();
    }

    /**
     * 恢复指定任务，首次延迟只在服务启动注册时生效
     * @param key 任务唯一标识
     * @return true表示恢复成功
     */
    public boolean resume(String key) {
        GameTimerTaskInfo taskInfo = taskMap.get(key);
        if (taskInfo == null || closed.get()) {
            return false;
        }
        synchronized (taskInfo) {
            long currentTime = System.currentTimeMillis();
            long nextTime = taskInfo.getTrigger().nextExecutionTime(taskInfo.context(currentTime));
            return scheduleAt(taskInfo, nextTime, true);
        }
    }

    /**
     * 立即执行指定任务，同一个任务不允许重叠执行
     * @param key 任务唯一标识
     * @return true表示任务已开始或已提交
     */
    public boolean triggerNow(String key) {
        GameTimerTaskInfo taskInfo = taskMap.get(key);
        if (taskInfo == null || closed.get() || !taskInfo.beginManualExecution()) {
            return false;
        }
        submitExecution(taskInfo);
        return true;
    }

    /**
     * 在运行期注册周期性任务
     * @param definition 任务定义
     * @return true表示注册成功
     */
    public boolean register(GameTimerDefinition definition) {
        if (definition == null || closed.get() || !initialized.get()) {
            return false;
        }

        GameTimerTaskInfo taskInfo = new GameTimerTaskInfo(definition.key(), definition.task(), resolveRunMethod(),
            definition.description(), definition.trigger(), definition.initialDelayMillis(),
            definition.maxConsecutiveFailures());
        if (taskMap.putIfAbsent(taskInfo.getKey(), taskInfo) != null) {
            return false;
        }
        try {
            registerInitialTask(taskInfo);
            return true;
        } catch (RuntimeException e) {
            taskMap.remove(taskInfo.getKey(), taskInfo);
            taskInfo.stop();
            LogTopic.ACTION.error(e, "GameTimerManager.dynamicRegisterError", "key", taskInfo.getKey());
            return false;
        }
    }

    /**
     * 移除指定任务，正在执行的方法不会被中断
     * @param key 任务唯一标识
     * @return true表示移除成功
     */
    public boolean remove(String key) {
        GameTimerTaskInfo taskInfo = taskMap.remove(key);
        if (taskInfo == null) {
            return false;
        }
        taskInfo.stop();
        LogTopic.ACTION.info("GameTimerManager.remove", "key", key);
        return true;
    }

    /**
     * 在线更新 Cron 触发规则
     * @param key 任务唯一标识
     * @param cron Spring六段式Cron表达式
     * @return true表示更新成功
     */
    public boolean updateCron(String key, String cron) {
        return updateCron(key, cron, "");
    }

    /**
     * 在线更新 Cron 触发规则
     * @param key 任务唯一标识
     * @param cron Spring六段式Cron表达式
     * @param zone Cron时区，空字符串表示系统默认时区
     * @return true表示更新成功
     */
    public boolean updateCron(String key, String cron, String zone) {
        if (!CronUtil.isValid(cron)) {
            return false;
        }
        try {
            ZoneId zoneId = StringUtils.isBlank(zone) ? ZoneId.systemDefault() : ZoneId.of(zone);
            return updateTrigger(key, new CronTimerTrigger(cron.trim(), zoneId));
        } catch (RuntimeException e) {
            LogTopic.ACTION.error(e, "GameTimerManager.updateCronError", "key", key, "cron", cron);
            return false;
        }
    }

    /**
     * 在线更新固定延迟触发规则
     * @param key 任务唯一标识
     * @param delay 固定延迟
     * @param timeUnit 时间单位
     * @return true表示更新成功
     */
    public boolean updateFixedDelay(String key, long delay, TimeUnit timeUnit) {
        if (delay <= 0L || timeUnit == null) {
            return false;
        }
        long delayMillis = timeUnit.toMillis(delay);
        if (delayMillis <= 0L) {
            return false;
        }
        return updateTrigger(key, new FixedDelayTimerTrigger(delayMillis));
    }

    /**
     * 在线更新动态时间提供者
     * @param key 任务唯一标识
     * @param provider 动态时间提供者
     * @param description 规则描述
     * @return true表示更新成功
     */
    public boolean updateDynamic(String key, NextExecutionTimeProvider provider, String description) {
        if (provider == null) {
            return false;
        }
        return updateTrigger(key, new DynamicTimerTrigger(provider, description));
    }

    /**
     * 获取指定任务运行快照
     * @param key 任务唯一标识
     * @return 任务快照，不存在时返回null
     */
    public GameTimerTaskSnapshot getTask(String key) {
        GameTimerTaskInfo taskInfo = taskMap.get(key);
        return taskInfo == null ? null : taskInfo.snapshot();
    }

    /**
     * 获取全部任务运行快照
     * @return 按任务key排序的任务快照
     */
    public List<GameTimerTaskSnapshot> getTasks() {
        return taskMap.values().stream().map(GameTimerTaskInfo::snapshot)
            .sorted(Comparator.comparing(GameTimerTaskSnapshot::key)).toList();
    }

    /** 停止全部任务，停止后不可恢复 */
    public void shutdown() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        taskMap.values().forEach(GameTimerTaskInfo::stop);
        LogTopic.ACTION.info("GameTimerManager.shutdown", "taskCount", taskMap.size());
    }

    /**
     * 容器关闭时先停止周期性任务，再由 ServerEventListener 关闭线程池
     * @param event 容器关闭事件
     */
    @Order(0)
    @EventListener(ContextClosedEvent.class)
    public void onClosed(ContextClosedEvent event) {
        shutdown();
    }

    /**
     * 扫描当前 Spring 容器中的周期性任务
     * @return key与任务信息映射
     */
    private Map<String, GameTimerTaskInfo> discoverTasks() {
        Map<String, GameTimerTaskInfo> discoveredTasks = new ConcurrentHashMap<>();
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            Object bean = getBean(beanName);
            if (bean == null) {
                continue;
            }

            Class<?> targetClass = AopUtils.getTargetClass(bean);
            GameTimer classTimer = AnnotatedElementUtils.findMergedAnnotation(targetClass, GameTimer.class);
            if (classTimer != null && supportsCurrentService(classTimer.services())) {
                putDiscoveredTask(discoveredTasks, buildClassTaskInfo(bean, targetClass, classTimer));
            }

            Map<Method, GameTimer> scheduledMethods = MethodIntrospector.selectMethods(targetClass,
                (MethodIntrospector.MetadataLookup<GameTimer>) method ->
                    AnnotatedElementUtils.findMergedAnnotation(method, GameTimer.class));
            for (Map.Entry<Method, GameTimer> entry : scheduledMethods.entrySet()) {
                GameTimer scheduled = entry.getValue();
                if (!supportsCurrentService(scheduled.services())) {
                    continue;
                }
                GameTimerTaskInfo taskInfo = buildTaskInfo(bean, entry.getKey(), scheduled);
                putDiscoveredTask(discoveredTasks, taskInfo);
            }
        }
        return discoveredTasks;
    }

    /**
     * 添加扫描到的任务并校验key唯一性
     * @param discoveredTasks 已扫描任务
     * @param taskInfo 待添加任务
     */
    private void putDiscoveredTask(Map<String, GameTimerTaskInfo> discoveredTasks, GameTimerTaskInfo taskInfo) {
        GameTimerTaskInfo oldTask = discoveredTasks.putIfAbsent(taskInfo.getKey(), taskInfo);
        if (oldTask != null) {
            throw new IllegalStateException("游戏周期性任务key重复: " + taskInfo.getKey());
        }
    }

    /**
     * 获取 Spring Bean，无法在当前阶段创建的 Bean 不参与任务扫描
     * @param beanName Spring Bean名称
     * @return Spring Bean
     */
    private Object getBean(String beanName) {
        try {
            return applicationContext.getBean(beanName);
        } catch (BeansException e) {
            LogTopic.ACTION.warn("GameTimerManager.skipBean", "beanName", beanName, "reason", e.getMessage());
            return null;
        }
    }

    /**
     * 构建并校验类级任务信息
     * @param bean 类级任务Spring Bean
     * @param targetClass 类级任务实际类型
     * @param scheduled 任务注解
     * @return 任务信息
     */
    private GameTimerTaskInfo buildClassTaskInfo(Object bean, Class<?> targetClass, GameTimer scheduled) {
        if (!AbstractGameTimer.class.isAssignableFrom(targetClass)) {
            throw new IllegalArgumentException(
                "类级@GameTimer必须继承AbstractGameTimer: " + targetClass.getName());
        }
        try {
            Method runMethod = AbstractGameTimer.class.getMethod("run");
            Method invocableMethod = AopUtils.selectInvocableMethod(runMethod, bean.getClass());
            return buildTaskInfo(bean, invocableMethod, scheduled, targetClass.getName() + "#execute");
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("无法获取AbstractGameTimer.run方法", e);
        }
    }

    /**
     * 构建并校验任务信息
     * @param bean 持有任务方法的Spring Bean
     * @param method 标记注解的方法
     * @param scheduled 任务注解
     * @return 任务信息
     */
    private GameTimerTaskInfo buildTaskInfo(Object bean, Method method, GameTimer scheduled) {
        String description = method.getDeclaringClass().getName() + "#" + method.getName();
        return buildTaskInfo(bean, method, scheduled, description);
    }

    /**
     * 构建并校验任务信息
     * @param bean 持有任务方法的Spring Bean
     * @param method 可调用任务方法
     * @param scheduled 任务注解
     * @param description 任务来源描述
     * @return 任务信息
     */
    private GameTimerTaskInfo buildTaskInfo(Object bean, Method method, GameTimer scheduled, String description) {
        String key = StringUtils.trim(scheduled.key());
        if (StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("游戏周期性任务key不能为空: " + method);
        }
        if (!Modifier.isPublic(method.getModifiers()) || method.getParameterCount() != 0
            || method.getReturnType() != void.class) {
            throw new IllegalArgumentException("游戏周期性任务方法必须是public void无参方法: " + method);
        }
        if (scheduled.initialDelay() < 0L) {
            throw new IllegalArgumentException("游戏周期性任务initialDelay不能小于0: " + key);
        }
        if (scheduled.maxConsecutiveFailures() < 0) {
            throw new IllegalArgumentException("游戏周期性任务maxConsecutiveFailures不能小于0: " + key);
        }

        long initialDelayMillis = scheduled.timeUnit().toMillis(scheduled.initialDelay());
        TimerTrigger trigger = createTrigger(key, scheduled);
        Method invocableMethod = AopUtils.selectInvocableMethod(method, bean.getClass());
        return new GameTimerTaskInfo(key, bean, invocableMethod, description, trigger, initialDelayMillis,
            scheduled.maxConsecutiveFailures());
    }

    /**
     * 根据注解创建触发规则
     * @param key 任务唯一标识
     * @param scheduled 任务注解
     * @return 任务触发规则
     */
    private TimerTrigger createTrigger(String key, GameTimer scheduled) {
        String cron = StringUtils.trim(scheduled.cron());
        boolean hasCron = StringUtils.isNotBlank(cron);
        boolean hasFixedDelay = scheduled.fixedDelay() != -1L;
        boolean hasDynamic = scheduled.nextTimeProvider() != NextExecutionTimeProvider.None.class;
        int triggerCount = (hasCron ? 1 : 0) + (hasFixedDelay ? 1 : 0) + (hasDynamic ? 1 : 0);
        if (triggerCount != 1) {
            throw new IllegalArgumentException(
                "游戏周期性任务cron、fixedDelay和nextTimeProvider必须且只能配置一个: " + key);
        }

        if (hasCron) {
            ZoneId zoneId = StringUtils.isBlank(scheduled.zone()) ? ZoneId.systemDefault()
                : ZoneId.of(scheduled.zone());
            return new CronTimerTrigger(cron, zoneId);
        }
        if (hasDynamic) {
            NextExecutionTimeProvider provider = applicationContext.getBean(scheduled.nextTimeProvider());
            return new DynamicTimerTrigger(provider, scheduled.nextTimeProvider().getName());
        }

        long fixedDelayMillis = scheduled.timeUnit().toMillis(scheduled.fixedDelay());
        if (scheduled.fixedDelay() <= 0L || fixedDelayMillis <= 0L) {
            throw new IllegalArgumentException("游戏周期性任务fixedDelay转换为毫秒后必须大于0: " + key);
        }
        return new FixedDelayTimerTrigger(fixedDelayMillis);
    }

    /**
     * 判断任务是否支持当前服务
     * @param services 任务支持的服务类型
     * @return true表示当前服务需要加载
     */
    private boolean supportsCurrentService(ServiceType[] services) {
        ServiceType currentService = ServiceType.curr();
        return Arrays.stream(services)
            .anyMatch(service -> service == ServiceType.ALL || service == currentService);
    }

    /**
     * 注册任务的首次执行
     * @param taskInfo 任务信息
     */
    private void registerInitialTask(GameTimerTaskInfo taskInfo) {
        long currentTime = System.currentTimeMillis();
        long nextTime = taskInfo.getTrigger().firstExecutionTime(taskInfo.context(currentTime),
            taskInfo.getInitialDelayMillis());
        if (!scheduleAt(taskInfo, nextTime, true)) {
            throw new IllegalStateException("游戏周期性任务首次注册失败: " + taskInfo.getKey());
        }
        LogTopic.ACTION.info("GameTimerManager.register", "key", taskInfo.getKey(), "expression",
            taskInfo.getTrigger().expression(), "nextTime", nextTime);
    }

    /**
     * 在指定时间调度任务
     * @param taskInfo 任务信息
     * @param executionTime 执行时间戳
     * @param fromPaused 是否从暂停状态发起调度
     * @return true表示调度成功
     */
    private boolean scheduleAt(GameTimerTaskInfo taskInfo, long executionTime, boolean fromPaused) {
        synchronized (taskInfo) {
            if (closed.get() || !taskInfo.prepareSchedule(executionTime, fromPaused)) {
                return false;
            }
            try {
                long delay = Math.max(executionTime - System.currentTimeMillis(), 0L);
                ScheduledFuture<?> future = scheduledExecutor.schedule(() -> fire(taskInfo), delay,
                    TimeUnit.MILLISECONDS);
                taskInfo.bindFuture(future);
                return true;
            } catch (RuntimeException e) {
                taskInfo.scheduleFailed();
                throw e;
            }
        }
    }

    /**
     * 原子替换指定任务的触发规则
     * @param key 任务唯一标识
     * @param newTrigger 新触发规则
     * @return true表示更新成功
     */
    private boolean updateTrigger(String key, TimerTrigger newTrigger) {
        GameTimerTaskInfo taskInfo = taskMap.get(key);
        if (taskInfo == null || closed.get()) {
            return false;
        }

        synchronized (taskInfo) {
            if (!taskInfo.canUpdateTrigger()) {
                return false;
            }
            boolean wasScheduled = taskInfo.isScheduled();
            TimerTrigger oldTrigger = taskInfo.replaceTrigger(newTrigger);
            try {
                if (wasScheduled) {
                    long currentTime = System.currentTimeMillis();
                    long nextTime = newTrigger.nextExecutionTime(taskInfo.context(currentTime));
                    if (nextTime <= 0L || !scheduleAt(taskInfo, nextTime, true)) {
                        throw new IllegalStateException("新触发规则没有可用的下次执行时间");
                    }
                }
                taskInfo.confirmTriggerUpdate();
                LogTopic.ACTION.info("GameTimerManager.updateTrigger", "key", key, "expression",
                    newTrigger.expression());
                return true;
            } catch (Throwable e) {
                taskInfo.restoreTrigger(oldTrigger);
                restoreOldSchedule(taskInfo, oldTrigger, wasScheduled);
                LogTopic.ACTION.error(e, "GameTimerManager.updateTriggerError", "key", key, "expression",
                    newTrigger.expression());
                return false;
            }
        }
    }

    /**
     * 触发规则更新失败后恢复原调度
     * @param taskInfo 任务信息
     * @param oldTrigger 原触发规则
     * @param wasScheduled 更新前是否正在等待调度
     */
    private void restoreOldSchedule(GameTimerTaskInfo taskInfo, TimerTrigger oldTrigger, boolean wasScheduled) {
        if (!wasScheduled || closed.get()) {
            return;
        }
        try {
            long currentTime = System.currentTimeMillis();
            long nextTime = oldTrigger.nextExecutionTime(taskInfo.context(currentTime));
            if (nextTime > 0L) {
                scheduleAt(taskInfo, nextTime, true);
            }
        } catch (Throwable restoreError) {
            LogTopic.ACTION.error(restoreError, "GameTimerManager.restoreTriggerError", "key", taskInfo.getKey());
        }
    }

    /**
     * 处理到期任务，仅向业务线程池提交执行
     * @param taskInfo 任务信息
     */
    private void fire(GameTimerTaskInfo taskInfo) {
        if (closed.get() || !taskInfo.beginScheduledExecution()) {
            return;
        }
        submitExecution(taskInfo);
    }

    /**
     * 向服务线程池提交任务
     * @param taskInfo 任务信息
     */
    private void submitExecution(GameTimerTaskInfo taskInfo) {
        try {
            serverExecutor.execute(() -> executeTask(taskInfo));
        } catch (Throwable e) {
            completeTask(taskInfo, e, false);
        }
    }

    /**
     * 反射执行任务方法
     * @param taskInfo 任务信息
     */
    private void executeTask(GameTimerTaskInfo taskInfo) {
        Throwable error = null;
        invokeBeforeInterceptors(taskInfo.context(System.currentTimeMillis()));
        try {
            taskInfo.getMethod().invoke(taskInfo.getHolder());
        } catch (InvocationTargetException e) {
            error = e.getTargetException();
        } catch (Throwable e) {
            error = e;
        } finally {
            completeTask(taskInfo, error, true);
        }
    }

    /**
     * 执行任务前置拦截器，拦截器异常不会阻断业务任务
     * @param context 任务运行上下文
     */
    private void invokeBeforeInterceptors(GameTimerContext context) {
        for (GameTimerInterceptor interceptor : interceptors) {
            try {
                interceptor.beforeExecute(context);
            } catch (Throwable e) {
                LogTopic.ACTION.error(e, "GameTimerManager.beforeInterceptorError", "key", context.key(),
                    "interceptor", interceptor.getClass().getName());
            }
        }
    }

    /**
     * 执行任务后置拦截器，按前置拦截器相反顺序回调
     * @param context 任务运行上下文
     * @param error 任务异常
     */
    private void invokeAfterInterceptors(GameTimerContext context, Throwable error) {
        for (int i = interceptors.size() - 1; i >= 0; i--) {
            GameTimerInterceptor interceptor = interceptors.get(i);
            try {
                interceptor.afterExecute(context, error);
            } catch (Throwable e) {
                LogTopic.ACTION.error(e, "GameTimerManager.afterInterceptorError", "key", context.key(),
                    "interceptor", interceptor.getClass().getName());
            }
        }
    }

    /**
     * 完成任务并根据触发规则注册下一次执行
     * @param taskInfo 任务信息
     * @param error 执行异常，成功时为null
     * @param invokeAfterInterceptor 是否执行后置拦截器
     */
    private void completeTask(GameTimerTaskInfo taskInfo, Throwable error, boolean invokeAfterInterceptor) {
        boolean shouldReschedule = taskInfo.completeExecution(error);
        if (invokeAfterInterceptor) {
            invokeAfterInterceptors(taskInfo.context(System.currentTimeMillis()), error);
        }
        if (error != null) {
            LogTopic.ACTION.error(error, "GameTimerManager.executeError", "key", taskInfo.getKey());
        }
        if (!shouldReschedule) {
            return;
        }
        if (closed.get()) {
            taskInfo.stop();
            return;
        }

        try {
            long currentTime = System.currentTimeMillis();
            long nextTime = taskInfo.getTrigger().nextExecutionTime(taskInfo.context(currentTime));
            if (nextTime <= 0L) {
                taskInfo.stop();
                LogTopic.ACTION.warn("GameTimerManager.noNextTime", "key", taskInfo.getKey());
                return;
            }
            scheduleAt(taskInfo, nextTime, false);
        } catch (Throwable e) {
            taskInfo.pause();
            LogTopic.ACTION.error(e, "GameTimerManager.rescheduleError", "key", taskInfo.getKey());
        }
    }

    /**
     * 获取 Runnable.run 方法
     * @return Runnable.run方法
     */
    private static Method resolveRunMethod() {
        try {
            return Runnable.class.getMethod("run");
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("无法获取Runnable.run方法", e);
        }
    }
}
