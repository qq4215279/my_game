package com.mumu.game.core.utils;

import static org.mockito.Mockito.when;

import com.mumu.game.core.clock.util.TimeUtil;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.mumu.game.core.clock.GameClock;

/**
 * TimeUtilTest
 * 静态时间工具测试
 * @author liuzhen
 * @version 1.0.0 2026/8/9 16:54
 */
public class TimeUtilTest {

    /** 验证能够通过静态方法获取系统、游戏和玩家时间 */
    @Test
    public void staticMethods_returnClockTime() {
        GameClock gameClock = Mockito.mock(GameClock.class);
        when(gameClock.systemTimeMillis()).thenReturn(1_000L);
        when(gameClock.gameTimeMillis()).thenReturn(5_000L);
        when(gameClock.playerTimeMillis(1001L)).thenReturn(8_000L);

        try (MockedStatic<SpringContextUtils> springContext = Mockito.mockStatic(SpringContextUtils.class)) {
            springContext.when(() -> SpringContextUtils.getBean(GameClock.class)).thenReturn(gameClock);

            Assert.assertEquals(TimeUtil.systemNow(), 1_000L);
            Assert.assertEquals(TimeUtil.now(), 5_000L);
            Assert.assertEquals(TimeUtil.playerNow(1001L), 8_000L);
            Assert.assertEquals(TimeUtil.nowDate().getTime(), 5_000L);
        }
    }
}
