/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.clock.controller;

import java.util.function.Supplier;

import com.mumu.game.business.system.luban.SystemSwitch;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mumu.game.core.clock.GameClock;
import com.mumu.game.core.clock.vo.ClockInfoVO;
import com.mumu.game.core.clock.vo.ClockUpdateRequestVO;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.http.HttpResult;

/**
 * GameClockController
 * 游戏时间HTTP管理入口
 * @author liuzhen
 * @version 1.0.0 2026/8/9 16:00
 */
@RestController
@RequestMapping("/admin/clock")
public class GameClockController {

    /** 游戏时间服务 */
    @Resource
    private GameClock gameClock;

    /**
     * 获取当前系统时间、游戏时间和指定玩家时间
     * @param playerId 玩家ID
     * @return HTTP响应
     */
    @GetMapping("/current/{playerId}")
    public HttpResult getCurrentTime(@PathVariable long playerId) {
        if (playerId <= 0L) {
            return HttpResult.error("玩家ID必须大于0");
        }
        return HttpResult.success()
                .add("playerId", playerId)
                .add("systemTime", gameClock.systemTimeMillis())
                .add("gameTime", gameClock.gameTimeMillis())
                .add("playerTime", gameClock.playerTimeMillis(playerId));
    }

    /**
     * 获取游戏时间
     * @return HTTP响应
     */
    @GetMapping("/game")
    public HttpResult getGameTime() {
        return HttpResult.success()
            .add("gmEnabled", SystemSwitch.isGM())
            .add("clock", gameClock.gameSnapshot());
    }

    /**
     * 修改游戏时间
     * @param request 时间修改请求
     * @return HTTP响应
     */
    @PutMapping("/game")
    public HttpResult setGameTime(@RequestBody ClockUpdateRequestVO request) {
        if (request == null || request.targetTimeMillis() <= 0L) {
            return HttpResult.error("目标时间戳必须大于0");
        }
        return mutate("setGameTime", null,
            () -> gameClock.setGameTime(request.targetTimeMillis(), request.reason()));
    }

    /**
     * 重置游戏时间
     * @param reason 修改原因
     * @return HTTP响应
     */
    @DeleteMapping("/game")
    public HttpResult resetGameTime(@RequestParam(defaultValue = "") String reason) {
        return mutate("resetGameTime", null, () -> gameClock.resetGameTime(reason));
    }

    /**
     * 获取玩家时间
     * @param playerId 玩家ID
     * @return HTTP响应
     */
    @GetMapping("/players/{playerId}")
    public HttpResult getPlayerTime(@PathVariable long playerId) {
        if (playerId <= 0L) {
            return HttpResult.error("玩家ID必须大于0");
        }
        return HttpResult.success().add("clock", gameClock.playerSnapshot(playerId));
    }

    /**
     * 修改玩家时间
     * @param playerId 玩家ID
     * @param request 时间修改请求
     * @return HTTP响应
     */
    @PutMapping("/players/{playerId}")
    public HttpResult setPlayerTime(
        @PathVariable long playerId,
        @RequestBody ClockUpdateRequestVO request) {
        if (playerId <= 0L || request == null || request.targetTimeMillis() <= 0L) {
            return HttpResult.error("玩家ID和目标时间戳必须大于0");
        }
        return mutate("setPlayerTime", playerId,
            () -> gameClock.setPlayerTime(playerId, request.targetTimeMillis(), request.reason()));
    }

    /**
     * 重置玩家时间
     * @param playerId 玩家ID
     * @param reason 修改原因
     * @return HTTP响应
     */
    @DeleteMapping("/players/{playerId}")
    public HttpResult resetPlayerTime(
        @PathVariable long playerId,
        @RequestParam(defaultValue = "") String reason) {
        if (playerId <= 0L) {
            return HttpResult.error("玩家ID必须大于0");
        }
        return mutate("resetPlayerTime", playerId, () -> gameClock.resetPlayerTime(playerId, reason));
    }

    /**
     * 重置全部玩家时间
     * @param reason 修改原因
     * @return HTTP响应
     */
    @DeleteMapping("/players")
    public HttpResult resetAllPlayerTime(@RequestParam(defaultValue = "") String reason) {
        try {
            int resetCount = gameClock.resetAllPlayerTime(reason);
            LogTopic.ACTION.info("GameClockController.resetAllPlayerTime", "resetCount", resetCount,
                "reason", reason);
            return HttpResult.success("操作成功").add("resetCount", resetCount);
        } catch (RuntimeException e) {
            return HttpResult.error(e.getMessage());
        }
    }

    /**
     * 执行时间修改并记录操作日志
     * @param operation 操作类型
     * @param playerId 玩家ID
     * @param action 修改动作
     * @return HTTP响应
     */
    private HttpResult mutate(String operation, Long playerId, Supplier<ClockInfoVO> action) {
        try {
            ClockInfoVO snapshot = action.get();
            LogTopic.ACTION.info("GameClockController.mutate", "operation", operation,
                "playerId", playerId, "clock", snapshot);
            return HttpResult.success("操作成功").add("clock", snapshot);
        } catch (RuntimeException e) {
            return HttpResult.error(e.getMessage());
        }
    }
}
