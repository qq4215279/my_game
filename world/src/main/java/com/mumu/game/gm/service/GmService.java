package com.mumu.game.gm.service;

import com.mumu.game.core.cmd.response.ResponseResult;

import java.util.List;

/**
 * GmService
 *
 * @author liuzhen
 * @version 1.0.0 2026/7/5 20:14
 */
public interface GmService {

    /**
     * 执行指令
     * @param playerId playerId
     * @param key key
     * @param args args
     * @return com.mumu.game.core.cmd.response.ResponseResult
     * @since 2026/7/5 20:18
     */
    ResponseResult executeGmCmd(long playerId, String key, List<String> args);
}
