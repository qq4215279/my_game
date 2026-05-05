package com.mumu.game.core.thread;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.LongAdder;

import com.mumu.game.constants.ThreadConstants;
import com.mumu.game.core.cmd.enums.Cmd;
import com.mumu.game.core.cmd.enums.ICmd;
import com.mumu.game.core.properties.CoreConfig;
import com.mumu.game.core.net.server.MessageContext;
import com.mumu.game.core.net.consts.ServiceType;
import com.mumu.game.core.thread2.GameEventExecutorGroup;

/**
 * ThreadPoolRouter
 * 线程选择器
 * @author liuzhen
 * @version 1.0.0 2025/3/16 15:59
 */
public class ThreadPoolRouter {
    /** 统计 - 添加玩家任务总数 */
    final LongAdder playerTaskCount = new LongAdder();
    /** 统计 - 添加服务任务总数 */
    final LongAdder serverTaskCount = new LongAdder();
    /** 统计 - 完成的任务总数 */
    final LongAdder processTaskCount = new LongAdder();
    /** 玩家线程 */
    final GameEventExecutorGroup playerExecutor;
    /** 服务线程 */
    @Deprecated
    final ThreadPoolExecutor serverExecutor;

    static final int MAX_QUEUE_SIZE = 5000;
    final LinkedBlockingQueue<Worker> workPool = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);

    public ThreadPoolRouter(GameEventExecutorGroup playerExecutor, ThreadPoolExecutor serverExecutor) {
        this.playerExecutor = playerExecutor;
        this.serverExecutor = serverExecutor;
        // 初始化任务队列池
        for (int i = 0; i < MAX_QUEUE_SIZE; i++) {
            workPool.add(newWorker());
        }
    }


    /** 消息路由 */
    public void autoExecute(MessageContext context, Runnable task) {
    autoExecute(getRouteId(context.getPlayerId()), context.getCmd(), task);
    }

    /** 消息路由 */
    private void autoExecute(Long key, ICmd cmd, Runnable task) {
        Worker worker = getWorker();
        worker.setCmd(cmd);
        worker.setTask(task);
        worker.begin();
        /* if (key == null) {
            serverExecutor.execute(worker);
            serverTaskCount.increment();
        } else {
            playerExecutor.execute(key, worker);
            playerTaskCount.increment();
        } */

        playerExecutor.submit(key, worker);
        playerTaskCount.increment();
    }

    private Worker getWorker() {
        Worker worker = workPool.poll();
        return worker != null ? worker : newWorker();
    }

    private Worker newWorker() {
        return new Worker(workPool, processTaskCount);
    }


    // ==========================> 工具方法 <=========================
    /** 获取路由Id，用于计算执行线程 */
    public static Long getRouteId(Long playerId) {
        Long key = playerId;
        if (key == null || ServiceType.GAME.notMyself()) {
            return key;
        }

        // 如果当前是游戏服，且玩家在桌子内，则路由到对应桌子
        /* Player player = PlayerManager.self().getPlayerOrNullable(key);
        if (player != null && player.getTableId() != 0) {
            return (long) player.getTableId();
        } */
        return key;
    }

    /** 当前是玩家线程 */
    public static boolean isPlayerThread() {
        return Thread.currentThread().getName().startsWith(ThreadConstants.THREAD_PREFIX_PLAYER);
    }

    /** 当前是指定的玩家线程 */
    public static boolean isPlayerThread(long key) {
        return isPlayerThread()
                && Thread.currentThread()
                .getName()
                .equals(
                        ThreadConstants.THREAD_PREFIX_PLAYER + CoreConfig.route(getRouteId(key)) + "-1");
    }
}
