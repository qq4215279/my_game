package com.mumu.game.core.clock;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.mumu.game.business.system.luban.SystemSwitch;
import com.mumu.game.core.clock.vo.ClockInfoVO;
import com.mumu.game.core.clock.consts.ClockSource;
import com.mumu.game.core.clock.controller.GameClockController;

/**
 * GameClockControllerTest
 * 游戏时间HTTP接口测试
 * @author liuzhen
 * @version 1.0.0 2026/8/9 16:00
 */
public class GameClockControllerTest {

    /** HTTP接口测试客户端 */
    private MockMvc mockMvc;
    /** 模拟游戏时间服务 */
    private GameClock gameClock;

    /** 初始化HTTP接口测试环境 */
    @BeforeMethod
    public void setUp() {
        gameClock = Mockito.mock(GameClock.class);
        GameClockController controller = new GameClockController();
        ReflectionTestUtils.setField(controller, "gameClock", gameClock);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /** 验证能够查询和修改游戏时间 */
    @Test
    public void gameEndpoints_queryAndUpdateGameTime() throws Exception {
        ClockInfoVO snapshot = snapshot(null, ClockSource.GAME, 5_000L);
        when(gameClock.gameSnapshot()).thenReturn(snapshot);
        when(gameClock.setGameTime(5_000L, "测试")).thenReturn(snapshot);

        mockMvc.perform(get("/admin/clock/game"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.gmEnabled").value(SystemSwitch.isGM()))
            .andExpect(jsonPath("$.data.clock.effectiveTime").value(5_000L));

        mockMvc.perform(put("/admin/clock/game")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"targetTimeMillis":5000,"reason":"测试"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));

        verify(gameClock).setGameTime(5_000L, "测试");
    }

    /** 验证能够同时查询系统、游戏和指定玩家的当前时间 */
    @Test
    public void currentTime_returnsAllClockTimes() throws Exception {
        when(gameClock.systemTimeMillis()).thenReturn(1_000L);
        when(gameClock.gameTimeMillis()).thenReturn(5_000L);
        when(gameClock.playerTimeMillis(1001L)).thenReturn(8_000L);

        mockMvc.perform(get("/admin/clock/current/1001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.playerId").value(1001L))
            .andExpect(jsonPath("$.data.systemTime").value(1_000L))
            .andExpect(jsonPath("$.data.gameTime").value(5_000L))
            .andExpect(jsonPath("$.data.playerTime").value(8_000L));

        verify(gameClock).playerTimeMillis(1001L);
    }

    /** 验证玩家时间修改和全部重置接口 */
    @Test
    public void playerEndpoints_updateAndResetPlayerTime() throws Exception {
        ClockInfoVO snapshot = snapshot(1001L, ClockSource.PLAYER, 8_000L);
        when(gameClock.setPlayerTime(1001L, 8_000L, "测试玩家")).thenReturn(snapshot);
        when(gameClock.resetAllPlayerTime("清理测试")).thenReturn(2);

        mockMvc.perform(put("/admin/clock/players/1001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"targetTimeMillis":8000,"reason":"测试玩家"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.clock.source").value("PLAYER"));

        mockMvc.perform(delete("/admin/clock/players").param("reason", "清理测试"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.resetCount").value(2));

        verify(gameClock).setPlayerTime(1001L, 8_000L, "测试玩家");
        verify(gameClock).resetAllPlayerTime("清理测试");
    }

    /**
     * 创建测试时间快照
     * @param playerId 玩家ID
     * @param source 时间来源
     * @param effectiveTime 有效时间戳
     * @return 测试时间快照
     */
    private ClockInfoVO snapshot(Long playerId, ClockSource source, long effectiveTime) {
        return new ClockInfoVO(playerId, source, 1_000L, effectiveTime,
            effectiveTime - 1_000L, 1L, 1_000L, "测试");
    }
}
