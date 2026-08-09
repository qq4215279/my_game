package com.mumu.game.core.timer;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import com.mumu.game.core.timer.bo.GameTimerContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.mumu.game.core.timer.core.trigger.CronTimerTrigger;
import com.mumu.game.core.timer.core.trigger.DynamicTimerTrigger;
import com.mumu.game.core.timer.core.trigger.FixedDelayTimerTrigger;
import com.mumu.game.core.timer.util.CronUtil;

/**
 * TimerTriggerTest
 * 周期性任务触发规则测试
 * @author liuzhen
 * @version 1.0.0 2026/8/9 11:26
 */
public class TimerTriggerTest {

    /** 验证 Cron 表达式合法性校验 */
    @Test
    public void cronUtil_validatesExpression() {
        Assert.assertTrue(CronUtil.isValid("*/5 * * * * *"));
        Assert.assertTrue(CronUtil.isValid(" 0 0 12 * * * "));
        Assert.assertFalse(CronUtil.isValid("0 0 12 * *"));
        Assert.assertFalse(CronUtil.isValid(""));
        Assert.assertFalse(CronUtil.isValid(null));
    }

    /** 验证 Cron 规则能够计算首次和后续执行时间 */
    @Test
    public void cronTrigger_calculatesNextExecutionTime() {
        CronTimerTrigger trigger = new CronTimerTrigger("0 0 * * * *", ZoneId.of("UTC"));
        long currentTime = Instant.parse("2026-08-09T10:15:00Z").toEpochMilli();

        long firstTime = trigger.firstExecutionTime(currentTime, 0L);
        long delayedFirstTime = trigger.firstExecutionTime(currentTime, 60 * 60 * 1000L);

        Assert.assertEquals(firstTime, Instant.parse("2026-08-09T11:00:00Z").toEpochMilli());
        Assert.assertEquals(delayedFirstTime, Instant.parse("2026-08-09T12:00:00Z").toEpochMilli());
        Assert.assertEquals(trigger.nextExecutionTime(firstTime),
            Instant.parse("2026-08-09T12:00:00Z").toEpochMilli());
    }

    /** 验证固定延迟规则从任务完成时间重新计时 */
    @Test
    public void fixedDelayTrigger_usesCompletionTime() {
        FixedDelayTimerTrigger trigger = new FixedDelayTimerTrigger(5_000L);
        long currentTime = 1_000L;

        Assert.assertEquals(trigger.firstExecutionTime(currentTime, 2_000L), 3_000L);
        Assert.assertEquals(trigger.nextExecutionTime(10_000L), 15_000L);
        Assert.assertEquals(trigger.expression(), "fixedDelay:5000ms");
    }

    /** 验证动态触发规则能够读取完整任务上下文 */
    @Test
    public void dynamicTrigger_usesTaskContext() {
        DynamicTimerTrigger trigger = new DynamicTimerTrigger(
            context -> context.currentTime() + context.executionCount() + 1_000L, "test-provider");
        GameTimerContext context = new GameTimerContext("dynamic", 10_000L, 0L, 0L, 0L, 5L, 0L, 0);

        Assert.assertEquals(trigger.nextExecutionTime(context), 11_005L);
        Assert.assertEquals(trigger.firstExecutionTime(context, 500L), 11_505L);
        Assert.assertEquals(trigger.expression(), "dynamic:test-provider");
    }

    /** 验证 Cron 工具能够预览指定时区的执行时间 */
    @Test
    public void cronUtil_previewsTimesWithZone() {
        ZonedDateTime start = ZonedDateTime.parse("2026-08-09T10:15:00Z");
        List<ZonedDateTime> times = CronUtil.getNextTimes("0 0 * * * *", 2, start);

        Assert.assertEquals(times.size(), 2);
        Assert.assertEquals(times.get(0), ZonedDateTime.parse("2026-08-09T11:00:00Z"));
        Assert.assertEquals(times.get(1), ZonedDateTime.parse("2026-08-09T12:00:00Z"));
    }
}
