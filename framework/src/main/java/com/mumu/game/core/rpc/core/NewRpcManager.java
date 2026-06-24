package com.mumu.game.core.rpc.core;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mumu.game.core.cmd.enums.RpcCmd;
import com.mumu.game.core.log.LogAction;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.net.consts.ServiceType;
import com.mumu.game.core.net.helper.MessageSender;
import com.mumu.game.core.net.server.MessageContext;
import com.mumu.game.core.net.session.PlayerManager;
import com.mumu.game.core.thread.ThreadPoolRouter;
import com.mumu.game.proto.message.system.message.GameMessagePackage;
import com.mumu.game.rpcproto.AbstractRpcRequestMessage;
import com.mumu.game.rpcproto.AbstractRpcResponseMessage;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * NewRpcManager
 * rpc管理器
 * @author liuzhen
 * @version 1.0.0 2026/6/24 16:05
 */
@Component
public class NewRpcManager {
    /** log */
    private static final LogTopic log = LogTopic.ACTION;

    /** 消息路由器 */
    private static ThreadPoolRouter threadPoolRouter;

    /** 超时时间(毫秒值) - 默认2s */
    private static final long TIME_OUT = 2000L;
    /** 自增协议id */
    private static final AtomicInteger INNER_REQUEST_ID = new AtomicInteger(0);

    @Resource
    public void setThreadPoolRouter(ThreadPoolRouter threadPoolRouter) {
        NewRpcManager.threadPoolRouter = threadPoolRouter;
    }

    /** seq 与 RpcFuture 映射 */
    private static final Cache<Integer, RpcFuture<AbstractRpcResponseMessage>> FUTURE_CACHE =
            // 2分钟超时过期
            // CacheBuilder.newBuilder().expireAfterWrite(2L, TimeUnit.MINUTES)
            CacheBuilder.newBuilder().expireAfterWrite(30L, TimeUnit.SECONDS)
                    .removalListener((removalNotification) -> {
                        if (removalNotification.getValue() != null) {
                            ((RpcFuture<?>) removalNotification.getValue()).setExpire();
                        }
                    }).build();


    /**
     * incrementAndGetSeq
     * @return int
     * @since 2025/6/7 16:43
     */
    public static int incrementAndGetSeq() {
        return INNER_REQUEST_ID.incrementAndGet();
    }

    /**
     * 同步发送rpc请求，并获得返回值
     * @param playerId playerId
     * @param cmd cmd
     * @param rpcMsg rpcMsg
     * @return com.game.rpcProto.RpcResult
     * @since 2025/6/7 16:44
     */
    public static RpcResult<? extends AbstractRpcResponseMessage> sendRpcRequest(long playerId, RpcCmd cmd, AbstractRpcRequestMessage rpcMsg) {
        return sendRpcRequest(null, playerId, cmd, rpcMsg);
    }

    /**
     * 同步发送rpc请求，并获得返回值
     * @param server server
     * @param playerId playerId
     * @param cmd cmd
     * @param rpcMsg rpcMsg
     * @return com.game.rpcProto.RpcResult
     * @since 2025/6/7 16:44
     */
    public static RpcResult<? extends AbstractRpcResponseMessage> sendRpcRequest(ServiceType server, long playerId, RpcCmd cmd, AbstractRpcRequestMessage rpcMsg) {
        long startTime = System.currentTimeMillis();
        boolean error = false;
        boolean finish = false;

        // send rpc request
        RpcFuture<AbstractRpcResponseMessage> future = doSendRpcRequestAsync(server, playerId, cmd, rpcMsg);

        RpcResult<? extends AbstractRpcResponseMessage> result;
        try {
            future.await(TIME_OUT);
            // 是否主动抛异常？
            if (future.getCause() != null) {
                throw future.getCause();
            }

            finish = true;
            result = future.getResult();
        } catch (RuntimeException exception) {
            error = true;
            // log.error(exception, PlayerManager.self().getPlayer(playerId), LogAction.RPC, "sendRpcRequest", "finish", finish, "error", error, "time", (System.currentTimeMillis() - startTime));
            throw exception;
        } finally {
            if (finish) {
                FUTURE_CACHE.invalidate(future.getSeq());
                // log.debug(playerId, ConfigSwitchEnum.LOG_CMD, LogAction.RPC, "sendRpcRequest", "finish", finish, "error", error, "time", (System.currentTimeMillis() - startTime));
            }
        }

        return result;
    }

    /**
     * 异步发送rpc请求
     * @param playerId playerId
     * @param cmd cmd
     * @param rpcMsg rpcMsg
     * @param callback 回调
     * @since 2025/6/7 16:49
     */
    public static void sendRpcRequestAsync(long playerId, RpcCmd cmd, AbstractRpcRequestMessage rpcMsg, RpcCallBack callback) {
        sendRpcRequestAsync(null, playerId, cmd, rpcMsg, callback);
    }

    /**
     * 异步发送rpc请求
     * @param server server
     * @param playerId playerId
     * @param cmd cmd
     * @param rpcMsg rpcMsg
     * @param callback 回调
     * @since 2025/6/7 16:50
     */
    public static void sendRpcRequestAsync(ServiceType server, long playerId, RpcCmd cmd, AbstractRpcRequestMessage rpcMsg, RpcCallBack callback) {
        RpcFuture<AbstractRpcResponseMessage> future = doSendRpcRequestAsync(server, playerId, cmd, rpcMsg);
        future.setCallback(callback);
    }

    /**
     * 异步发送rpc请求
     * @param playerId playerId
     * @param cmd cmd
     * @param rpcMsg rpcMsg
     * @return com.game.framework.core.rpc.core.RpcFuture<com.game.rpcProto.AbstractRpcResponseMessage>
     * @since 2025/6/7 16:50
     */
    public static void sendRpcRequestAsync(long playerId, RpcCmd cmd, AbstractRpcRequestMessage rpcMsg) {
        sendRpcRequestAsync(null, playerId, cmd, rpcMsg);
    }

    /**
     * 异步发送rpc请求
     * @param server server
     * @param playerId playerId
     * @param cmd cmd
     * @param rpcMsg rpcMsg
     * @return com.game.framework.core.rpc.core.RpcFuture<com.game.rpcProto.AbstractRpcResponseMessage>
     * @since 2025/6/7 16:50
     */
    public static void sendRpcRequestAsync(ServiceType server, long playerId, RpcCmd cmd, AbstractRpcRequestMessage rpcMsg) {
        doSendRpcRequestAsync(server, playerId, cmd, rpcMsg);
    }

    /**
     * doSendRpcRequestAsync
     * @param server server
     * @param playerId playerId
     * @param cmd cmd
     * @param rpcMsg rpcMsg
     * @return com.game.framework.core.rpc.core.RpcFuture<com.game.rpcProto.AbstractRpcResponseMessage>
     * @since 2025/6/7 16:50
     */
    private static RpcFuture<AbstractRpcResponseMessage> doSendRpcRequestAsync(ServiceType server, long playerId, RpcCmd cmd, AbstractRpcRequestMessage rpcMsg) {

        GameMessagePackage reqPackage = new GameMessagePackage();
        int seq = reqPackage.getSeq();

        RpcFuture<AbstractRpcResponseMessage> future = new RpcFuture<>(playerId, seq, reqPackage, threadPoolRouter);
        FUTURE_CACHE.put(seq, future);

        boolean error = false;
        try {
            // 发送rpc
            if (server == null) {
                server = cmd.getServiceType();
            }
            MessageSender.sendToPlayerServer(server, reqPackage);

        } catch (RuntimeException exception) {
            error = true;
            // log.error(exception, PlayerManager.self().getPlayer(playerId), LogAction.RPC, "doSendRpcRequestAsync", "error", error);
            throw exception;
        }

        return future;
    }


    // ==========================================>

    /**
     * 处理rpc请求响应
     * @param context context
     * @since 2025/6/7 16:04
     */
    public void handleRpcResponse(MessageContext context) {
        RpcFuture<AbstractRpcResponseMessage> future = FUTURE_CACHE.getIfPresent(context.getSeq());
        if (null != future) {
            future.setResult(context);
            FUTURE_CACHE.invalidate(future.getSeq());
        }
    }

}
