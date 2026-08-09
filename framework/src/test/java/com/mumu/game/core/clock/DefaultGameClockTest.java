package com.mumu.game.core.clock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.mumu.game.core.clock.vo.ClockInfoVO;
import com.mumu.game.core.clock.config.GameClockProperties;
import com.mumu.game.core.clock.consts.ClockSource;
import com.mumu.game.core.clock.event.GameClockChangedEvent;
import com.mumu.game.core.clock.event.PlayerClockChangedEvent;
import com.mumu.game.core.utils.SpringContextUtils;
import com.mumu.game.business.system.luban.SystemSwitch;

/**
 * DefaultGameClockTest
 * 游戏时间服务测试
 * @author liuzhen
 * @version 1.0.0 2026/8/9 16:00
 */
public class DefaultGameClockTest {

    /** 测试系统时钟 */
    private MutableClock systemClock;
    /** 已发布事件 */
    private List<Object> events;
    /** 被测试的游戏时间服务 */
    private DefaultGameClock gameClock;

    /** 初始化测试环境 */
    @BeforeMethod
    public void setUp() {
        systemClock = new MutableClock(1_000L);
        events = new ArrayList<>();
        GameClockProperties properties = new GameClockProperties();
        properties.setGmEnabled(true);
        new SpringContextUtils().setPublisher(events::add);
        gameClock = new DefaultGameClock(properties, systemClock);
    }

    /** 验证游戏时间以偏移量保存并随系统时间继续流逝 */
    @Test
    public void gameTime_usesJvmOffsetAndKeepsMoving() {
        ClockInfoVO changed = gameClock.setGameTime(5_000L, "测试游戏时间");

        Assert.assertEquals(changed.source(), ClockSource.GAME);
        Assert.assertEquals(changed.offsetMillis(), 4_000L);
        Assert.assertEquals(gameClock.gameTimeMillis(), 5_000L);

        systemClock.setMillis(1_500L);
        Assert.assertEquals(gameClock.gameTimeMillis(), 5_500L);
        Assert.assertEquals(events.size(), 1);
        Assert.assertTrue(events.getFirst() instanceof GameClockChangedEvent);
    }

    /** 验证玩家时间优先于游戏时间，清理后逐级回退 */
    @Test
    public void playerTime_hasHigherPriorityAndFallsBack() {
        gameClock.setGameTime(5_000L, "设置游戏时间");
        gameClock.setPlayerTime(1001L, 8_000L, "设置玩家时间");

        Assert.assertEquals(gameClock.playerSnapshot(1001L).source(), ClockSource.PLAYER);
        Assert.assertEquals(gameClock.playerTimeMillis(1001L), 8_000L);

        systemClock.setMillis(2_000L);
        Assert.assertEquals(gameClock.playerTimeMillis(1001L), 9_000L);
        Assert.assertEquals(gameClock.playerTimeMillis(1002L), 6_000L);

        Assert.assertEquals(gameClock.resetPlayerTime(1001L, "清理玩家时间").source(), ClockSource.GAME);
        Assert.assertEquals(gameClock.playerTimeMillis(1001L), 6_000L);
        Assert.assertEquals(gameClock.resetGameTime("清理游戏时间").source(), ClockSource.SYSTEM);
        Assert.assertEquals(gameClock.playerTimeMillis(1001L), 2_000L);
        Assert.assertTrue(events.stream().anyMatch(PlayerClockChangedEvent.class::isInstance));
    }

    /** 验证GM开关关闭时拒绝修改时间 */
    @Test
    public void mutation_isRejectedWhenGmSwitchDisabled() {
        GameClockProperties properties = new GameClockProperties();
        DefaultGameClock disabledClock = new DefaultGameClock(properties, systemClock);

        try (MockedStatic<SystemSwitch> systemSwitch = Mockito.mockStatic(SystemSwitch.class)) {
            systemSwitch.when(SystemSwitch::notGM).thenReturn(true);
            IllegalStateException error = Assert.expectThrows(IllegalStateException.class,
                () -> disabledClock.setGameTime(5_000L, "不允许的修改"));
            Assert.assertTrue(error.getMessage().contains("未开启"));
        }
    }

    /**
     * MutableClock
     * 测试使用的可变系统时钟
     * @author liuzhen
     * @version 1.0.0 2026/8/9 16:00
     */
    private static final class MutableClock extends Clock {

        /** 当前时间戳 */
        private long millis;

        /**
         * 创建可变时钟
         * @param millis 初始时间戳
         */
        private MutableClock(long millis) {
            this.millis = millis;
        }

        /**
         * 修改当前时间
         * @param millis 新时间戳
         */
        private void setMillis(long millis) {
            this.millis = millis;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis);
        }

        @Override
        public long millis() {
            return millis;
        }
    }
}
