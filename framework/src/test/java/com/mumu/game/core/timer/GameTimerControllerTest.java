package com.mumu.game.core.timer;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.mumu.game.core.timer.controller.GameTimerController;
import com.mumu.game.core.timer.core.GameTimerManager;

/**
 * GameTimerControllerTest
 * 游戏周期性任务HTTP接口测试
 * @author liuzhen
 * @version 1.0.0 2026/8/9 14:38
 */
public class GameTimerControllerTest {

    /** HTTP接口测试客户端 */
    private MockMvc mockMvc;
    /** 模拟任务管理器 */
    private GameTimerManager timerManager;

    /** 初始化HTTP接口测试环境 */
    @BeforeMethod
    public void setUp() {
        timerManager = Mockito.mock(GameTimerManager.class);
        GameTimerController controller = new GameTimerController();
        ReflectionTestUtils.setField(controller, "timerManager", timerManager);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /** 验证能够通过HTTP查询任务列表 */
    @Test
    public void tasks_returnsTaskList() throws Exception {
        when(timerManager.getTasks()).thenReturn(List.of());

        mockMvc.perform(get("/admin/timer/tasks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.tasks").isArray());
    }

    /** 验证能够通过HTTP立即执行任务 */
    @Test
    public void trigger_executesTask() throws Exception {
        when(timerManager.triggerNow("test")).thenReturn(true);

        mockMvc.perform(post("/admin/timer/tasks/test/trigger"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.msg").value("操作成功"));

        verify(timerManager).triggerNow("test");
    }

    /** 验证能够通过HTTP更新Cron规则 */
    @Test
    public void updateCron_updatesTaskTrigger() throws Exception {
        when(timerManager.updateCron("test", "0 0 12 * * *", "UTC")).thenReturn(true);

        mockMvc.perform(put("/admin/timer/tasks/test/cron")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"cron":"0 0 12 * * *","zone":"UTC"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));

        verify(timerManager).updateCron("test", "0 0 12 * * *", "UTC");
    }

    /** 验证Cron校验和时间预览接口 */
    @Test
    public void cronEndpoints_validateAndPreview() throws Exception {
        String request = """
            {"cron":"0 0 * * * *","count":2,"zone":"UTC"}
            """;

        mockMvc.perform(post("/admin/timer/cron/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.valid").value(true));

        mockMvc.perform(post("/admin/timer/cron/next")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.valid").value(true))
            .andExpect(jsonPath("$.data.zone").value("UTC"))
            .andExpect(jsonPath("$.data.times.length()").value(2));
    }
}
