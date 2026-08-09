/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.timer.controller;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.timer.vo.CronPreviewRequestVO;
import com.mumu.game.core.timer.vo.CronUpdateRequestVO;
import com.mumu.game.core.timer.vo.FixedDelayUpdateRequestVO;
import com.mumu.game.core.timer.core.GameTimerManager;
import com.mumu.game.core.timer.bo.GameTimerTaskSnapshot;
import com.mumu.game.core.timer.util.CronUtil;
import com.mumu.game.http.HttpResult;

/**
 * GameTimerController
 * 游戏周期性任务HTTP管理入口
 * @author liuzhen
 * @version 1.0.0 2026/8/9 14:38
 */
@RestController
@RequestMapping("/admin/timer")
public class GameTimerController {

    /** 默认预览后续执行时间数量 */
    private static final int DEFAULT_PREVIEW_COUNT = 5;
    /** 单次允许预览的最大执行时间数量 */
    private static final int MAX_PREVIEW_COUNT = 50;

    /** 游戏周期性任务管理器 */
    @Resource
    private GameTimerManager timerManager;


    /**
     * 获取全部任务运行快照
     * @return HTTP响应
     */
    @GetMapping("/tasks")
    public HttpResult getTasks() {
        return HttpResult.success().add("tasks", timerManager.getTasks());
    }

    /**
     * 获取指定任务运行快照
     * @param key 任务唯一标识
     * @return HTTP响应
     */
    @GetMapping("/tasks/{key}")
    public HttpResult getTask(@PathVariable String key) {
        GameTimerTaskSnapshot task = timerManager.getTask(key);
        return task == null ? HttpResult.error("任务不存在: " + key) : HttpResult.success().add("task", task);
    }

    /**
     * 立即执行指定任务
     * @param key 任务唯一标识
     * @return HTTP响应
     */
    @PostMapping("/tasks/{key}/trigger")
    public HttpResult trigger(@PathVariable String key) {
        return operationResult("trigger", key, timerManager.triggerNow(key));
    }

    /**
     * 暂停指定任务
     * @param key 任务唯一标识
     * @return HTTP响应
     */
    @PostMapping("/tasks/{key}/pause")
    public HttpResult pause(@PathVariable String key) {
        return operationResult("pause", key, timerManager.pause(key));
    }

    /**
     * 恢复指定任务
     * @param key 任务唯一标识
     * @return HTTP响应
     */
    @PostMapping("/tasks/{key}/resume")
    public HttpResult resume(@PathVariable String key) {
        return operationResult("resume", key, timerManager.resume(key));
    }

    /**
     * 更新指定任务的Cron规则
     * @param key 任务唯一标识
     * @param request Cron更新请求
     * @return HTTP响应
     */
    @PutMapping("/tasks/{key}/cron")
    public HttpResult updateCron(@PathVariable String key, @RequestBody CronUpdateRequestVO request) {
        if (request == null || !CronUtil.isValid(request.cron())) {
            return HttpResult.error("Cron表达式不合法");
        }
        return operationResult("updateCron", key, timerManager.updateCron(key, request.cron(), request.zone()));
    }

    /**
     * 更新指定任务的固定延迟规则
     * @param key 任务唯一标识
     * @param request 固定延迟更新请求
     * @return HTTP响应
     */
    @PutMapping("/tasks/{key}/fixed-delay")
    public HttpResult updateFixedDelay(@PathVariable String key, @RequestBody FixedDelayUpdateRequestVO request) {
        if (request == null || request.delay() <= 0L || request.timeUnit() == null) {
            return HttpResult.error("固定延迟参数不合法");
        }
        return operationResult("updateFixedDelay", key,
            timerManager.updateFixedDelay(key, request.delay(), request.timeUnit()));
    }

    /**
     * 删除指定任务
     * @param key 任务唯一标识
     * @return HTTP响应
     */
    @DeleteMapping("/tasks/{key}")
    public HttpResult remove(@PathVariable String key) {
        return operationResult("remove", key, timerManager.remove(key));
    }

    /**
     * 校验Cron表达式
     * @param request Cron请求
     * @return HTTP响应
     */
    @PostMapping("/cron/validate")
    public HttpResult validateCron(@RequestBody CronPreviewRequestVO request) {
        boolean valid = request != null && CronUtil.isValid(request.cron());
        return HttpResult.success().add("valid", valid);
    }

    /**
     * 预览Cron后续执行时间
     * @param request Cron预览请求
     * @return HTTP响应
     */
    @PostMapping("/cron/next")
    public HttpResult previewCron(@RequestBody CronPreviewRequestVO request) {
        if (request == null || !CronUtil.isValid(request.cron())) {
            return HttpResult.error("Cron表达式不合法");
        }
        int count = request.count() == null ? DEFAULT_PREVIEW_COUNT : request.count();
        if (count <= 0 || count > MAX_PREVIEW_COUNT) {
            return HttpResult.error("预览次数必须在1到" + MAX_PREVIEW_COUNT + "之间");
        }

        try {
            ZoneId zoneId = request.zone() == null || request.zone().isBlank() ? ZoneId.systemDefault()
                : ZoneId.of(request.zone());
            List<ZonedDateTime> times = CronUtil.getNextTimes(request.cron(), count, ZonedDateTime.now(zoneId));
            return HttpResult.success().add("valid", true).add("zone", zoneId.getId()).add("times", times);
        } catch (RuntimeException e) {
            return HttpResult.error("Cron预览失败: " + e.getMessage());
        }
    }

    /**
     * 构建任务操作响应并记录操作日志
     * @param action 操作类型
     * @param key 任务唯一标识
     * @param success 是否操作成功
     * @return HTTP响应
     */
    private HttpResult operationResult(String action, String key, boolean success) {
        LogTopic.ACTION.info("GameTimerController.operation", "operation", action, "key", key, "success", success);
        return success ? HttpResult.success("操作成功") : HttpResult.error("操作失败，任务不存在或当前状态不允许");
    }
}
