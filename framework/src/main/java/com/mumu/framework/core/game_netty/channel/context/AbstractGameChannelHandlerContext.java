/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.game_netty.channel.context;

import com.mumu.common.proto.message.system.message.GameMessagePackage;
import com.mumu.framework.core.cmd.response.ResponseResult;
import com.mumu.framework.core.game_netty.channel.GameChannel;
import com.mumu.framework.core.game_netty.channel.future.DefaultGameChannelPromise;
import com.mumu.framework.core.game_netty.channel.future.GameChannelFuture;
import com.mumu.framework.core.game_netty.channel.future.GameChannelPromise;
import com.mumu.framework.core.game_netty.channel.handler.GameChannelHandler;
import com.mumu.framework.core.game_netty.channel.handler.GameChannelInboundHandler;
import com.mumu.framework.core.game_netty.channel.handler.GameChannelOutboundHandler;

import io.netty.channel.DefaultChannelPipeline;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PromiseNotificationUtil;
import io.netty.util.internal.ThrowableUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * AbstractGameChannelHandlerContext GameChannelHandler 的上下文类
 * 
 * @author liuzhen
 * @version 1.0.0 2025/3/30 16:51
 */
public abstract class AbstractGameChannelHandlerContext {
    static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultChannelPipeline.class);

    /** 前一个节点 */
    volatile AbstractGameChannelHandlerContext prev;
    /** 后一个节点 */
    volatile AbstractGameChannelHandlerContext next;

    /** context名称 */
    private String name;
    /** 管道 */
    protected final GameChannelPipeline pipeline;
    /**  */
    final EventExecutor executor;
    /**  */
    private final boolean inbound;
    /**  */
    private final boolean outbound;

    public AbstractGameChannelHandlerContext(GameChannelPipeline pipeline, EventExecutor executor, String name,
        boolean inbound, boolean outbound) {
        this.name = ObjectUtil.checkNotNull(name, "name");
        this.pipeline = pipeline;
        this.executor = executor;
        this.inbound = inbound;
        this.outbound = outbound;
    }

    /**
     * 获取handler
     * 
     * @return com.mygame.gateway.message.channel.handler.GameChannelHandler
     * @date 2024/6/26 14:16
     */
    public abstract GameChannelHandler handler();

    /**
     *
     * @return com.mygame.gateway.message.channel.context.AbstractGameChannelHandlerContext
     * @date 2024/6/26 14:07
     */
    public AbstractGameChannelHandlerContext fireChannelInactive() {
        invokeChannelInactive(findContextInbound());
        return this;
    }

    static void invokeChannelInactive(final AbstractGameChannelHandlerContext next) {
        EventExecutor executor = next.executor();
        if (executor.inEventLoop()) {
            next.invokeChannelInactive();
        } else {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    next.invokeChannelInactive();
                }
            });
        }
    }

    private void invokeChannelInactive() {
        try {
            ((GameChannelInboundHandler)handler()).channelInactive(this);
        } catch (Throwable t) {
            notifyHandlerException(t);
        }
    }

    private AbstractGameChannelHandlerContext findContextInbound() {
        AbstractGameChannelHandlerContext ctx = this;
        do {
            ctx = ctx.next;
        } while (!ctx.inbound);

        return ctx;
    }

    /**
     *
     * @param cause cause
     * @return com.mygame.gateway.message.channel.context.AbstractGameChannelHandlerContext
     * @date 2024/6/26 14:08
     */
    public AbstractGameChannelHandlerContext fireExceptionCaught(final Throwable cause) {
        invokeExceptionCaught(next, cause);
        return this;
    }

    static void invokeExceptionCaught(final AbstractGameChannelHandlerContext next, final Throwable cause) {
        ObjectUtil.checkNotNull(cause, "cause");
        EventExecutor executor = next.executor();
        if (executor.inEventLoop()) {
            next.invokeExceptionCaught(cause);
        } else {
            try {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        next.invokeExceptionCaught(cause);
                    }
                });
            } catch (Throwable t) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Failed to submit an exceptionCaught() event.", t);
                    logger.warn("The exceptionCaught() event that was failed to submit was:", cause);
                }
            }
        }
    }

    private void invokeExceptionCaught(final Throwable cause) {
        try {
            handler().exceptionCaught(this, cause);
        } catch (Throwable error) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                    "An exception {}" + "was thrown by a user handler's exceptionCaught() "
                        + "method while handling the following exception:",
                    ThrowableUtil.stackTraceToString(error), cause);
            } else if (logger.isWarnEnabled()) {
                logger.warn("An exception '{}' [enable DEBUG level for full stacktrace] "
                    + "was thrown by a user handler's exceptionCaught() "
                    + "method while handling the following exception:", error, cause);
            }
        }

    }

    /**
     *
     * @param playerId playerId
     * @param promise promise
     * @return com.mygame.gateway.message.channel.context.AbstractGameChannelHandlerContext
     * @date 2024/6/26 14:08
     */
    public AbstractGameChannelHandlerContext fireChannelRegistered(long playerId, GameChannelPromise promise) {
        invokeChannelRegistered(findContextInbound(), playerId, promise);
        return this;
    }

    static void invokeChannelRegistered(final AbstractGameChannelHandlerContext next, long playerId,
        GameChannelPromise promise) {
        EventExecutor executor = next.executor();
        if (executor.inEventLoop()) {
            next.invokeChannelRegistered(playerId, promise);
        } else {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    next.invokeChannelRegistered(playerId, promise);
                }
            });
        }
    }

    private void invokeChannelRegistered(long playerId, GameChannelPromise promise) {
        try {
            ((GameChannelInboundHandler)handler()).channelRegister(this, playerId, promise);
        } catch (Throwable t) {
            notifyHandlerException(t);
        }
    }

    /**
     *
     * @param event event
     * @param promise promise
     * @return com.mygame.gateway.message.channel.context.AbstractGameChannelHandlerContext
     * @date 2024/6/26 14:09
     */
    public AbstractGameChannelHandlerContext fireUserEventTriggered(final Object event, Promise<Object> promise) {
        invokeUserEventTriggered(findContextInbound(), event, promise);
        return this;
    }

    static void invokeUserEventTriggered(final AbstractGameChannelHandlerContext next, final Object event,
        Promise<Object> promise) {
        ObjectUtil.checkNotNull(event, "event");
        EventExecutor executor = next.executor();
        if (executor.inEventLoop()) {
            next.invokeUserEventTriggered(event, promise);
        } else {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    next.invokeUserEventTriggered(event, promise);
                }
            });
        }
    }

    private void invokeUserEventTriggered(Object event, Promise<Object> promise) {
        try {
            ((GameChannelInboundHandler)handler()).userEventTriggered(this, event, promise);
        } catch (Throwable t) {
            notifyHandlerException(t);
        }
    }

    /**
     *
     * @param msg msg
     * @return com.mygame.gateway.message.channel.context.AbstractGameChannelHandlerContext
     * @date 2024/6/26 14:09
     */
    public AbstractGameChannelHandlerContext fireChannelRead(final Object msg) {
        invokeChannelRead(findContextInbound(), msg);
        return this;
    }

    static void invokeChannelRead(final AbstractGameChannelHandlerContext next, final Object msg) {
        ObjectUtil.checkNotNull(msg, "msg");
        EventExecutor executor = next.executor();
        if (executor.inEventLoop()) {
            next.invokeChannelRead(msg);
        } else {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    next.invokeChannelRead(msg);
                }
            });
        }
    }

    private void invokeChannelRead(Object msg) {
        try {
            ((GameChannelInboundHandler)handler()).channelRead(this, msg);
        } catch (Throwable t) {
            notifyHandlerException(t);
        }

    }

    /**
     *
     * @param msg msg
     * @return com.mygame.gateway.message.channel.context.AbstractGameChannelHandlerContext
     * @date 2024/6/26 14:09
     */
    public AbstractGameChannelHandlerContext fireChannelReadRPCRequest(final GameMessagePackage msg) {
        invokeChannelReadRPCRequest(findContextInbound(), msg);
        return this;
    }

    static void invokeChannelReadRPCRequest(final AbstractGameChannelHandlerContext next,
        final GameMessagePackage msg) {
        ObjectUtil.checkNotNull(msg, "msg");
        EventExecutor executor = next.executor();
        if (executor.inEventLoop()) {
            next.invokeChannelReadRPCRequest(msg);
        } else {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    next.invokeChannelReadRPCRequest(msg);
                }
            });
        }
    }

    private void invokeChannelReadRPCRequest(GameMessagePackage msg) {
        try {
            ((GameChannelInboundHandler)handler()).channelReadRpcRequest(this, msg);
        } catch (Throwable t) {
            notifyHandlerException(t);
        }

    }

    private void notifyHandlerException(Throwable cause) {
        if (inExceptionCaught(cause)) {
            if (logger.isWarnEnabled()) {
                logger.warn("An exception was thrown by a user handler " + "while handling an exceptionCaught event",
                    cause);
            }
            return;
        }

        invokeExceptionCaught(cause);
    }

    private static boolean inExceptionCaught(Throwable cause) {
        do {
            StackTraceElement[] trace = cause.getStackTrace();
            if (trace != null) {
                for (StackTraceElement t : trace) {
                    if (t == null) {
                        break;
                    }
                    if ("exceptionCaught".equals(t.getMethodName())) {
                        return true;
                    }
                }
            }

            cause = cause.getCause();
        } while (cause != null);

        return false;
    }

    /**
     *
     * @param msg msg
     * @return com.mygame.gateway.message.channel.future.GameChannelFuture
     * @date 2024/6/26 14:11
     */
    public GameChannelFuture writeAndFlush(ResponseResult msg) {
        return writeAndFlush(msg, newPromise());
    }

    /**
     *
     * @param msg msg
     * @param promise promise
     * @return com.mygame.gateway.message.channel.future.GameChannelFuture
     * @date 2024/6/26 14:11
     */
    public GameChannelFuture writeAndFlush(ResponseResult msg, GameChannelPromise promise) {
        AbstractGameChannelHandlerContext next = findContextOutbound();
        EventExecutor executor = next.executor();
        if (executor.inEventLoop()) {
            next.invokeWrite(msg, promise);
        } else {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    next.invokeWrite(msg, promise);
                }
            });
        }
        return promise;
    }

    private void invokeWrite(ResponseResult msg, GameChannelPromise promise) {
        try {
            ((GameChannelOutboundHandler)handler()).writeAndFlush(this, msg, promise);
        } catch (Throwable t) {
            notifyOutboundHandlerException(t, promise);
        }
    }

    /**
     *
     * @return com.mygame.gateway.message.channel.future.GameChannelPromise
     * @date 2024/6/26 14:11
     */
    public GameChannelPromise newPromise() {
        return new DefaultGameChannelPromise(gameChannel(), this.executor());
    }

    /**
     *
     * @param msg msg
     * @param promise promise
     * @return void
     * @date 2024/6/26 14:12
     */
    public void writeRPCMessage(GameMessagePackage msg, Promise<GameMessagePackage> promise) {
        AbstractGameChannelHandlerContext next = findContextOutbound();
        EventExecutor executor = next.executor();
        if (executor.inEventLoop()) {
            next.invokeWriteRPCMessage(msg, promise);
        } else {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    next.invokeWriteRPCMessage(msg, promise);
                }
            });
        }
    }

    private AbstractGameChannelHandlerContext findContextOutbound() {
        AbstractGameChannelHandlerContext ctx = this;
        do {
            ctx = ctx.prev;
        } while (!ctx.outbound);
        return ctx;
    }

    private void invokeWriteRPCMessage(GameMessagePackage msg, Promise<GameMessagePackage> callback) {
        try {
            ((GameChannelOutboundHandler)handler()).writeRPCMessage(this, msg, callback);
        } catch (Throwable t) {
            notifyOutboundHandlerException(t, callback);
        }
    }

    private static void notifyOutboundHandlerException(Throwable cause, Promise<?> promise) {
        PromiseNotificationUtil.tryFailure(promise, cause, logger);
    }

    /**
     *
     * @return com.mygame.gateway.message.channel.future.GameChannelFuture
     * @date 2024/6/26 14:12
     */
    public GameChannelFuture close() {
        return this.close(new DefaultGameChannelPromise(this.gameChannel()));
    }

    /**
     *
     * @param promise promise
     * @return com.mygame.gateway.message.channel.future.GameChannelFuture
     * @date 2024/6/26 14:12
     */
    public GameChannelFuture close(final GameChannelPromise promise) {
        final AbstractGameChannelHandlerContext next = findContextOutbound();
        EventExecutor executor = next.executor();
        if (executor.inEventLoop()) {
            next.invokeClose(promise);
        } else {
            safeExecute(executor, new Runnable() {
                @Override
                public void run() {
                    next.invokeClose(promise);
                }
            }, promise, null);
        }

        return promise;
    }

    private void invokeClose(GameChannelPromise promise) {
        try {
            ((GameChannelOutboundHandler)handler()).close(this, promise);
        } catch (Throwable t) {
            notifyOutboundHandlerException(t, promise);
        }

    }

    private static boolean safeExecute(EventExecutor executor, Runnable runnable, GameChannelPromise promise,
        Object msg) {
        try {
            executor.execute(runnable);
            return true;
        } catch (Throwable cause) {
            try {
                promise.setFailure(cause);
            } finally {
                if (msg != null) {
                    ReferenceCountUtil.release(msg);
                }
            }
            return false;
        }
    }

    public String name() {
        return name;
    }

    public GameChannelPipeline pipeline() {
        return pipeline;
    }

    public GameChannel gameChannel() {
        return pipeline.gameChannel();
    }

    public EventExecutor executor() {
        if (executor == null) {
            return gameChannel().executor();
        } else {
            return executor;
        }
    }

}
