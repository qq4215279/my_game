package com.mumu.game.core.rpc.core;

import com.mumu.game.core.net.server.MessageContext;
import com.mumu.game.core.thread.ThreadPoolRouter;
import com.mumu.game.expcetion.NetworkTimeoutException;
import com.mumu.game.proto.message.core.ErrorCode;
import com.mumu.game.proto.message.system.message.GameMessagePackage;
import com.mumu.game.rpcproto.AbstractRpcResponseMessage;
import lombok.Getter;

/**
 * RpcFuture
 * @author liuzhen
 * @version 1.0.0 2026/6/24 16:03
 */
public class RpcFuture <V extends AbstractRpcResponseMessage> {
    /** rpc 请求过期异常 */
    private static final RuntimeException EXPIRED_CAUSE = new NetworkTimeoutException("rpc request expired");
    /** rpc 请求超时异常 */
    private static final RuntimeException TIME_OUT_CAUSE = new NetworkTimeoutException("rpc request timeout");


    /** 是否已执行 */
    private volatile boolean isDone;
    /** 异常 */
    @Getter
    private RuntimeException cause;

    /** playerId */
    private final long playerId;
    /** seq */
    @Getter
    private final int seq;
    /** seq */
    private final GameMessagePackage requestProxy;
    /** threadPoolRouter */
    private final ThreadPoolRouter threadPoolRouter;

    /** 返回上下文 */
    private MessageContext context;
    /** 返回值 */
    @Getter
    private RpcResult<V> result;
    /** 回调 */
    private RpcCallBack callback;


    public RpcFuture(long playerId, int seq, GameMessagePackage reqPackage, ThreadPoolRouter threadPoolRouter) {
        this.playerId = playerId;
        this.seq = seq;
        this.requestProxy = reqPackage;
        this.threadPoolRouter = threadPoolRouter;
    }

    /**
     * 阻塞等待
     * @param timeout 阻塞超时时间
     * @since 2025/6/8 11:46
     */
    public void await(long timeout) {
        if (!this.isDone) {
            synchronized(this) {
                if (!this.isDone) {
                    try {
                        // 阻塞线程
                        this.wait(timeout);
                        if (this.isDone) {
                            return;
                        }

                        this.isDone = true;
                        // 超时异常
                        this.cause = TIME_OUT_CAUSE;
                        this.context =  MessageContext.of(requestProxy, null);
                        this.result = new RpcResult<>(playerId, ErrorCode.RPC_REQUEST_TIME_OUT);

                        // 打印输出
                        printRpcResultLog(context);

                        this.doCallback();
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }

    /**
     * 设置超时过期
     * @since 2025/6/8 11:46
     */
    public void setExpire() {
        if (!this.isDone) {
            this.isDone = true;
            this.cause = EXPIRED_CAUSE;
            this.context =  MessageContext.of(requestProxy, null);
            this.result = new RpcResult<>(playerId, ErrorCode.RPC_REQUEST_EXPIRED);

            // 打印输出
            printRpcResultLog(context);

            // 唤醒阻塞
            synchronized(this) {
                this.notifyAll();
            }

            this.doCallback();
        }
    }

    /**
     * 设置结果返回
     * @param context context
     * @since 2025/6/8 11:46
     */
    public void setResult(MessageContext context) {
        if (!this.isDone) {
            this.isDone = true;
            this.context = context;
            GameMessagePackage proxy = context.getMessagePackage();
            this.result = new RpcResult<>(proxy.getPlayerId(), proxy.getErrorCode(), context.getRpcResponseMsg());

            // 打印输出
            printRpcResultLog(context);

            // 唤醒阻塞
            synchronized(this) {
                this.notifyAll();
            }

            this.doCallback();
        }
    }

    /**
     * 打印响应日志日志
     * @param context context
     * @since 2025/6/9 18:01
     */
    private void printRpcResultLog(MessageContext context) {
        // 打印响应日志日志
        context.debug("rpcResult");
    }

    /**
     * 设置回调
     * @param callback 回调函数
     * @since 2025/6/8 11:46
     */
    public void setCallback(RpcCallBack callback) {
        this.callback = callback;
        if (this.isDone) {
            this.doCallback();
        }
    }

    /**
     * do回调执行
     * @since 2025/6/8 11:47
     */
    private void doCallback() {
        if (null != this.callback) {
            threadPoolRouter.autoExecute(context, () -> this.callback.callback(this));
        }
    }

}