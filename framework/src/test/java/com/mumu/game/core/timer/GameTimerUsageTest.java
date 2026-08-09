package com.mumu.game.core.timer;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.mumu.game.core.timer.anno.GameTimer;
import com.mumu.game.core.timer.bo.GameTimerContext;
import com.mumu.game.core.timer.core.AbstractGameTimer;
import com.mumu.game.core.timer.core.GameTimerManager;
import com.mumu.game.core.timer.core.NextExecutionTimeProvider;

/**
 * GameTimerUsageTest
 * 游戏周期性任务不同定义方式用例
 * @author liuzhen
 * @version 1.0.0 2026/8/9 15:24
 */
public class GameTimerUsageTest {

    /** 测试Spring容器 */
    private GenericApplicationContext context;
    /** 测试调度线程池 */
    private ScheduledExecutorService scheduledExecutor;
    /** 游戏周期性任务管理器 */
    private GameTimerManager timerManager;

    /** 初始化测试环境 */
    @BeforeMethod
    public void setUp() {
        context = new GenericApplicationContext();
        context.registerBean(MethodTimerBean.class);
        context.registerBean(ClassTimerTask.class);
        context.registerBean(UsageNextTimeProvider.class);
        context.refresh();

        scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        timerManager = new GameTimerManager(context, scheduledExecutor, Runnable::run, List.of());
        timerManager.autoInit();
    }

    /** 清理测试环境 */
    @AfterMethod
    public void tearDown() {
        timerManager.shutdown();
        scheduledExecutor.shutdownNow();
        context.close();
    }

    /** 验证方法级Cron、固定延迟和动态时间提供者三种定义方式 */
    @Test
    public void methodAnnotations_supportThreeTriggerTypes() {
        Assert.assertEquals(timerManager.getTask("usage_method_cron").expression(), "0 0 0 1 1 *");
        Assert.assertTrue(timerManager.getTask("usage_method_fixed_delay").expression().startsWith("fixedDelay:"));
        Assert.assertTrue(timerManager.getTask("usage_method_dynamic").expression().startsWith("dynamic:"));
    }

    /** 验证类级注解通过AbstractGameTimer执行重写逻辑 */
    @Test
    public void classAnnotation_executesAbstractTimerMethod() {
        ClassTimerTask classTimerTask = context.getBean(ClassTimerTask.class);

        Assert.assertNotNull(timerManager.getTask("usage_class_timer"));
        Assert.assertTrue(timerManager.pause("usage_class_timer"));
        Assert.assertTrue(timerManager.triggerNow("usage_class_timer"));
        Assert.assertEquals(classTimerTask.executionCount.get(), 1);
    }


    // 使用示例：
    /**
     * MethodTimerBean
     * 方法级周期性任务用例
     * @author liuzhen
     * @version 1.0.0 2026/8/9 15:24
     */
    public static class MethodTimerBean {

        /** 使用方式1：使用Cron表达式定义任务 */
        @GameTimer(key = "usage_method_cron", cron = "0 0 0 1 1 *")
        public void cronTask() {
        }

        /** 使用方式2：使用固定延迟定义任务 */
        @GameTimer(key = "usage_method_fixed_delay", fixedDelay = 1L, initialDelay = 1L,
            timeUnit = TimeUnit.DAYS)
        public void fixedDelayTask() {
        }

        /** 使用方式3：使用动态时间提供者定义任务 */
        @GameTimer(key = "usage_method_dynamic", nextTimeProvider = UsageNextTimeProvider.class)
        public void dynamicTask() {
        }
    }

    /**
     * ClassTimerTask
     * 使用方式4：类级周期性任务用例
     * @author liuzhen
     * @version 1.0.0 2026/8/9 15:24
     */
    @GameTimer(key = "usage_class_timer", fixedDelay = 1L, initialDelay = 1L, timeUnit = TimeUnit.DAYS)
    public static class ClassTimerTask extends AbstractGameTimer {

        /** 类级任务执行次数 */
        private final AtomicInteger executionCount = new AtomicInteger();

        @Override
        protected void execute() {
            executionCount.incrementAndGet();
        }
    }

    /**
     * UsageNextTimeProvider
     * 动态时间提供者用例
     * @author liuzhen
     * @version 1.0.0 2026/8/9 15:24
     */
    public static class UsageNextTimeProvider implements NextExecutionTimeProvider {

        @Override
        public long nextExecutionTime(GameTimerContext context) {
            return context.currentTime() + TimeUnit.DAYS.toMillis(1);
        }
    }
}
