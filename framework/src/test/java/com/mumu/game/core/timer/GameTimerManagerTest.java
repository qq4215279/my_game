package com.mumu.game.core.timer;

import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

import com.mumu.game.core.clock.GameClock;
import com.mumu.game.core.clock.consts.ClockType;
import com.mumu.game.core.clock.event.GameClockChangedEvent;
import com.mumu.game.core.timer.bo.GameTimerContext;
import com.mumu.game.core.timer.consts.GameTimerState;
import com.mumu.game.core.timer.bo.GameTimerDefinition;
import com.mumu.game.core.timer.core.GameTimerInterceptor;
import com.mumu.game.core.timer.core.GameTimerManager;
import com.mumu.game.core.timer.bo.GameTimerTaskSnapshot;
import com.mumu.game.core.timer.core.trigger.FixedDelayTimerTrigger;
import com.mumu.game.core.timer.core.NextExecutionTimeProvider;
import org.springframework.context.support.GenericApplicationContext;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.mumu.game.core.timer.anno.GameTimer;

/**
 * GameTimerManagerTest
 * 游戏周期性任务管理器测试
 * @author liuzhen
 * @version 1.0.0 2026/8/9 11:26
 */
public class GameTimerManagerTest {

    /** 测试使用的 Spring 容器 */
    private GenericApplicationContext context;
    /** 测试使用的调度线程池 */
    private ScheduledExecutorService scheduledExecutor;
    /** 被测试的任务管理器 */
    private GameTimerManager manager;
    /** 测试任务 Bean */
    private TestTimerBean timerBean;
    /** 测试执行拦截器 */
    private TestTimerInterceptor interceptor;
    /** 测试游戏时间 */
    private AtomicLong gameTime;

    /** 初始化测试环境 */
    @BeforeMethod
    public void setUp() {
        context = new GenericApplicationContext();
        context.registerBean(TestTimerBean.class);
        context.registerBean(TestNextTimeProvider.class);
        context.refresh();

        timerBean = context.getBean(TestTimerBean.class);
        interceptor = new TestTimerInterceptor();
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        long currentTime = System.currentTimeMillis();
        gameTime = new AtomicLong(currentTime);
        GameClock gameClock = Mockito.mock(GameClock.class);
        when(gameClock.systemTimeMillis()).thenAnswer(invocation -> System.currentTimeMillis());
        when(gameClock.gameTimeMillis()).thenAnswer(invocation -> gameTime.get());
        // 使用直接执行器，让管理API测试不依赖异步等待。
        manager = new GameTimerManager(context, scheduledExecutor, Runnable::run, List.of(interceptor), gameClock);
        manager.autoInit();
    }

    /** 清理测试环境 */
    @AfterMethod
    public void tearDown() {
        manager.shutdown();
        scheduledExecutor.shutdownNow();
        context.close();
    }

    /** 验证任务的暂停、手动执行和恢复流程 */
    @Test
    public void task_canPauseTriggerAndResume() {
        GameTimerTaskSnapshot initial = manager.getTask("timer_test_success");
        Assert.assertNotNull(initial);
        Assert.assertEquals(initial.state(), GameTimerState.SCHEDULED);

        Assert.assertTrue(manager.pause("timer_test_success"));
        Assert.assertEquals(manager.getTask("timer_test_success").state(), GameTimerState.PAUSED);

        Assert.assertTrue(manager.triggerNow("timer_test_success"));
        GameTimerTaskSnapshot manual = manager.getTask("timer_test_success");
        Assert.assertEquals(timerBean.successCount.get(), 1);
        Assert.assertEquals(manual.state(), GameTimerState.PAUSED);
        Assert.assertEquals(manual.executionCount(), 1L);
        Assert.assertEquals(manual.manualExecutionCount(), 1L);
        Assert.assertEquals(manual.successCount(), 1L);
        Assert.assertEquals(interceptor.beforeCount.get(), 1);
        Assert.assertEquals(interceptor.afterCount.get(), 1);

        Assert.assertTrue(manager.resume("timer_test_success"));
        Assert.assertEquals(manager.getTask("timer_test_success").state(), GameTimerState.SCHEDULED);
    }

    /** 验证任务异常被隔离并记录在运行快照中 */
    @Test
    public void task_failure_isRecordedAndIsolated() {
        Assert.assertTrue(manager.pause("timer_test_failure"));
        Assert.assertTrue(manager.triggerNow("timer_test_failure"));

        GameTimerTaskSnapshot snapshot = manager.getTask("timer_test_failure");
        Assert.assertEquals(snapshot.state(), GameTimerState.PAUSED);
        Assert.assertEquals(snapshot.executionCount(), 1L);
        Assert.assertEquals(snapshot.failureCount(), 1L);
        Assert.assertTrue(snapshot.lastError().contains("timer-test-error"));
    }

    /** 验证在线更新触发规则并保留非法更新前的规则 */
    @Test
    public void trigger_canUpdateAtRuntime() {
        GameTimerTaskSnapshot before = manager.getTask("timer_test_success");

        Assert.assertTrue(manager.updateCron("timer_test_success", "0 0 12 * * *", "UTC"));
        GameTimerTaskSnapshot updated = manager.getTask("timer_test_success");
        Assert.assertEquals(updated.expression(), "0 0 12 * * *");
        Assert.assertEquals(updated.triggerVersion(), before.triggerVersion() + 1);
        Assert.assertEquals(updated.state(), GameTimerState.SCHEDULED);

        Assert.assertFalse(manager.updateCron("timer_test_success", "invalid-cron", "UTC"));
        Assert.assertEquals(manager.getTask("timer_test_success").expression(), "0 0 12 * * *");

        Assert.assertFalse(manager.updateDynamic("timer_test_success", context -> 0L, "no-next-time"));
        Assert.assertEquals(manager.getTask("timer_test_success").expression(), "0 0 12 * * *");
        Assert.assertEquals(manager.getTask("timer_test_success").state(), GameTimerState.SCHEDULED);
    }

    /** 验证动态任务可以注册、更新和移除 */
    @Test
    public void dynamicTask_canRegisterUpdateAndRemove() {
        AtomicInteger count = new AtomicInteger();
        GameTimerDefinition definition = new GameTimerDefinition("runtime_timer", "运行期任务",
            count::incrementAndGet, new FixedDelayTimerTrigger(TimeUnit.DAYS.toMillis(1)), TimeUnit.DAYS.toMillis(1),
            0);

        Assert.assertTrue(manager.register(definition));
        Assert.assertNotNull(manager.getTask("runtime_timer"));
        Assert.assertTrue(manager.updateFixedDelay("runtime_timer", 2L, TimeUnit.DAYS));
        Assert.assertEquals(manager.getTask("runtime_timer").expression(),
            "fixedDelay:" + TimeUnit.DAYS.toMillis(2) + "ms");

        Assert.assertTrue(manager.pause("runtime_timer"));
        Assert.assertTrue(manager.triggerNow("runtime_timer"));
        Assert.assertEquals(count.get(), 1);
        Assert.assertTrue(manager.remove("runtime_timer"));
        Assert.assertNull(manager.getTask("runtime_timer"));
    }

    /** 验证动态时间提供者注解能够被扫描注册 */
    @Test
    public void dynamicProviderAnnotation_isDiscovered() {
        GameTimerTaskSnapshot snapshot = manager.getTask("timer_test_dynamic");

        Assert.assertNotNull(snapshot);
        Assert.assertTrue(snapshot.expression().startsWith("dynamic:"));
        Assert.assertEquals(snapshot.state(), GameTimerState.SCHEDULED);
        Assert.assertEquals(snapshot.clockType(), ClockType.GAME);
    }

    /** 验证游戏时间向前跨过触发点时仅补执行一次，并在回拨后跳过已执行点 */
    @Test
    public void gameClockChange_fireOnceAndAvoidDuplicateAfterRollback() {
        GameTimerTaskSnapshot initial = manager.getTask("timer_test_dynamic");
        long systemTaskNextTime = manager.getTask("timer_test_system_clock").nextExecutionTime();
        long oldTime = gameTime.get();
        long forwardTime = initial.nextExecutionTime() + 1L;
        gameTime.set(forwardTime);

        manager.onGameClockChanged(new GameClockChangedEvent(oldTime, forwardTime, 1L, "向前调整"));

        GameTimerTaskSnapshot afterForward = manager.getTask("timer_test_dynamic");
        Assert.assertEquals(timerBean.dynamicCount.get(), 1);
        Assert.assertEquals(afterForward.executionCount(), 1L);
        Assert.assertEquals(afterForward.manualExecutionCount(), 0L);
        Assert.assertEquals(afterForward.state(), GameTimerState.SCHEDULED);
        Assert.assertEquals(afterForward.lastExecutedScheduledTime(), initial.nextExecutionTime());
        Assert.assertEquals(manager.getTask("timer_test_system_clock").nextExecutionTime(), systemTaskNextTime);

        gameTime.set(oldTime);
        manager.onGameClockChanged(new GameClockChangedEvent(forwardTime, oldTime, 2L, "向后调整"));

        GameTimerTaskSnapshot afterRollback = manager.getTask("timer_test_dynamic");
        Assert.assertEquals(timerBean.dynamicCount.get(), 1);
        Assert.assertTrue(afterRollback.nextExecutionTime() > afterRollback.lastExecutedScheduledTime());
    }

    /** 验证连续失败达到阈值后任务自动暂停 */
    @Test
    public void consecutiveFailures_pauseTask() throws InterruptedException {
        GameTimerDefinition definition = new GameTimerDefinition("failure_limit_timer", "失败阈值任务",
            () -> {
                throw new IllegalStateException("failure-limit");
            }, new FixedDelayTimerTrigger(10L), 0L, 2);

        Assert.assertTrue(manager.register(definition));
        waitUntil(() -> {
            GameTimerTaskSnapshot snapshot = manager.getTask("failure_limit_timer");
            return snapshot != null && snapshot.state() == GameTimerState.PAUSED;
        }, 1_000L);

        GameTimerTaskSnapshot snapshot = manager.getTask("failure_limit_timer");
        Assert.assertEquals(snapshot.failureCount(), 2L);
        Assert.assertEquals(snapshot.consecutiveFailureCount(), 2);
        Assert.assertTrue(snapshot.pauseReason().contains("连续失败"));
    }

    /**
     * 等待测试条件成立
     * @param condition 测试条件
     * @param timeoutMillis 超时时间
     */
    private void waitUntil(BooleanSupplier condition, long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10L);
        }
        Assert.assertTrue(condition.getAsBoolean(), "等待周期性任务状态超时");
    }

    /**
     * TestTimerBean
     * 测试周期性任务
     * @author liuzhen
     * @version 1.0.0 2026/8/9 11:26
     */
    public static class TestTimerBean {

        /** 成功任务执行次数 */
        private final AtomicInteger successCount = new AtomicInteger();
        /** 动态时间任务执行次数 */
        private final AtomicInteger dynamicCount = new AtomicInteger();

        /** 正常执行的测试任务 */
        @GameTimer(key = "timer_test_success", fixedDelay = 1L, initialDelay = 1L,
            timeUnit = TimeUnit.DAYS)
        public void successTask() {
            successCount.incrementAndGet();
        }

        /** 执行失败的测试任务 */
        @GameTimer(key = "timer_test_failure", fixedDelay = 1L, initialDelay = 1L,
            timeUnit = TimeUnit.DAYS)
        public void failureTask() {
            throw new IllegalStateException("timer-test-error");
        }

        /** 使用动态时间提供者的测试任务 */
        @GameTimer(key = "timer_test_dynamic", nextTimeProvider = TestNextTimeProvider.class)
        public void dynamicTask() {
            dynamicCount.incrementAndGet();
        }

        /** 使用系统时间且不响应游戏时间调整的任务 */
        @GameTimer(key = "timer_test_system_clock", nextTimeProvider = TestNextTimeProvider.class,
            clockType = ClockType.SYSTEM)
        public void systemClockTask() {
        }
    }

    /**
     * TestNextTimeProvider
     * 测试动态时间提供者
     * @author liuzhen
     * @version 2.0.0 2026/8/9 12:19
     */
    public static class TestNextTimeProvider implements NextExecutionTimeProvider {

        @Override
        public long nextExecutionTime(GameTimerContext context) {
            return context.currentTime() + TimeUnit.DAYS.toMillis(1);
        }
    }

    /**
     * TestTimerInterceptor
     * 测试任务执行拦截器
     * @author liuzhen
     * @version 2.0.0 2026/8/9 12:19
     */
    private static class TestTimerInterceptor implements GameTimerInterceptor {

        /** 前置回调次数 */
        private final AtomicInteger beforeCount = new AtomicInteger();
        /** 后置回调次数 */
        private final AtomicInteger afterCount = new AtomicInteger();

        @Override
        public void beforeExecute(GameTimerContext context) {
            beforeCount.incrementAndGet();
        }

        @Override
        public void afterExecute(GameTimerContext context, Throwable error) {
            afterCount.incrementAndGet();
        }
    }
}
