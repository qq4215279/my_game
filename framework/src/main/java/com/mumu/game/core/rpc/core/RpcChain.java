package com.mumu.game.core.rpc.core;

import com.mumu.game.core.cmd.enums.RpcCmd;
import com.mumu.game.core.net.consts.ServiceType;
import com.mumu.game.rpcproto.AbstractRpcRequestMessage;
import com.mumu.game.rpcproto.AbstractRpcResponseMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * RpcChain
 * Rpc链子
 * 作用：用于同时发送多个异步RPC请求，并一起构建结果返回
 * @author liuzhen
 * @version 1.0.0 2026/6/24 16:04
 */
public class RpcChain {
    /** rpc异步请求链子列表 */
    List<CompletableFuture<RpcResult<?>>> chainList = new ArrayList<>();

    public RpcChain() {
    }

    public static RpcChain build() {
        return new RpcChain();
    }

    /**
     * 添加rpc请求链子
     * @param playerId playerId
     * @param cmd cmd
     * @param rpcMsg rpcMsg
     * @param callback callback
     * @return RpcChain
     * @since 2025/6/10 10:24
     */
    public RpcChain addChain(long playerId, RpcCmd cmd, AbstractRpcRequestMessage rpcMsg, RpcCallBack callback) {
        return addChain(null, playerId, cmd,  rpcMsg, callback);
    }

    /**
     * 添加rpc请求链子
     * @param server server
     * @param playerId playerId
     * @param cmd cmd
     * @param rpcMsg rpcMsg
     * @param callback callback
     * @return RpcChain
     * @since 2025/6/10 10:25
     */
    public RpcChain addChain(
            ServiceType server, long playerId, RpcCmd cmd, AbstractRpcRequestMessage rpcMsg, RpcCallBack callback) {
        CompletableFuture<RpcResult<? extends AbstractRpcResponseMessage>> completableFuture = new CompletableFuture<>();

        NewRpcManager.sendRpcRequestAsync(server, playerId, cmd, rpcMsg, rpcFuture -> {
            callback.callback(rpcFuture);
            // 回调收到结果后完成future
            completableFuture.complete(rpcFuture.getResult());
        });

        chainList.add(completableFuture);

        return this;
    }

    /**
     * 添加rpc请求链子
     * @param playerId playerId
     * @param cmd cmd
     * @param rpcMsg rpcMsg
     * @return RpcChain
     * @since 2025/6/10 10:25
     */
    public RpcChain addChain(long playerId, RpcCmd cmd, AbstractRpcRequestMessage rpcMsg) {
        addChain(null, playerId, cmd,  rpcMsg);
        return this;
    }

    /**
     * 添加rpc请求链子
     * @param server server
     * @param playerId playerId
     * @param cmd cmd
     * @param rpcMsg rpcMsg
     * @return RpcChain
     * @since 2025/6/10 10:25
     */
    public RpcChain addChain(ServiceType server, long playerId, RpcCmd cmd, AbstractRpcRequestMessage rpcMsg) {
        CompletableFuture<RpcResult<? extends AbstractRpcResponseMessage>> completableFuture = new CompletableFuture<>();

        NewRpcManager.sendRpcRequestAsync(server, playerId, cmd, rpcMsg, rpcFuture -> {
            // 回调收到结果后完成future
            completableFuture.complete(rpcFuture.getResult());
        });

        chainList.add(completableFuture);
        return this;
    }

    /**
     * 完成所有链子后异步回调，处理自定义业务逻辑
     * @param run run
     * @since 2025/6/10 10:25
     */
    public void asyncThenRun(Runnable run) {
        CompletableFuture<Void> completableFuture = CompletableFuture.allOf(chainList.toArray(new CompletableFuture[0]));
        completableFuture.thenRun(run);
    }

    /**
     * 同步等待所有任务完成后回调
     * @param run 最终回调
     * @since 2025/6/18 10:34
     */
    public void syncThenRun(Runnable run) {
        CompletableFuture<Void> completableFuture = CompletableFuture.allOf(chainList.toArray(new CompletableFuture[0]));
        completableFuture.thenRun(run).join();
    }

    /**
     * 同步等待完成所有任务
     * @since 2025/6/18 10:35
     */
    public void sync() {
        CompletableFuture<Void> completableFuture = CompletableFuture.allOf(chainList.toArray(new CompletableFuture[0]));
        completableFuture.join();
    }

}
