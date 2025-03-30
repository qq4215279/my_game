/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.game_netty.handler;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.mumu.common.proto.message.system.message.GameMessagePackage;
import com.mumu.framework.core.cmd.response.ResponseResult;
import com.mumu.framework.core.game_netty.channel.context.AbstractGameChannelHandlerContext;
import com.mumu.framework.core.game_netty.channel.future.GameChannelPromise;
import com.mumu.framework.core.game_netty.channel.handler.GameChannelInboundHandler;
import com.mumu.framework.core.game_netty.channel.handler.GameChannelOutboundHandler;

import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.Promise;

/**
 * GameChannelIdleStateHandler
 * GameChannel 空闲超时处理
 * @author liuzhen
 * @version 1.0.0 2025/3/30 17:35
 */
public class IdleStateGameChannelHandler implements GameChannelInboundHandler, GameChannelOutboundHandler {
    /** 延迟事件的延迟时间的最小值 */
    private static final long MIN_TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(1);
    /** 读取消息的空闲时间，单位纳秒 */
    private final long readerIdleTimeNanos;
    /** 写出消息的空闲时间，单位纳秒 */
    private final long writerIdleTimeNanos;
    /** 读取和写出消息的空闲时间，单位纳秒 */
    private final long allIdleTimeNanos;

    /** 读取消息的超时延时检测事件 */
    private ScheduledFuture<?> readerIdleTimeoutScheduled;
    /** 最近一次读取消息的时间 */
    private long lastReadTime;
    /** 写出消息的超时延时检测事件 */
    private ScheduledFuture<?> writerIdleTimeoutScheduled;
    /** 最近一次写出消息的时间 */
    private long lastWriteTime;
    /** 读写消息的超时检测事件 */
    private ScheduledFuture<?> allIdleTimeoutScheduled;

    /** 0 - none, 1 - initialized, 2 - destroyed */
    private byte state;
    // 要不然会有这样的情况，虽然GameChannel已被移除，但是当定时事件执行时，又会创建一个新的定时事件，导致这个对象不会被回收


    public IdleStateGameChannelHandler(int readerIdleTimeSeconds, int writerIdleTimeSeconds, int allIdleTimeSeconds) {
        this(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds, TimeUnit.SECONDS);
    }

    public IdleStateGameChannelHandler(long readerIdleTime, long writerIdleTime, long allIdleTime, TimeUnit unit) {
        if (unit == null) {
            throw new NullPointerException("unit");
        }

        if (readerIdleTime <= 0) {
            readerIdleTimeNanos = 0;
        } else {
            readerIdleTimeNanos = Math.max(unit.toNanos(readerIdleTime), MIN_TIMEOUT_NANOS);
        }

        if (writerIdleTime <= 0) {
            writerIdleTimeNanos = 0;
        } else {
            writerIdleTimeNanos = Math.max(unit.toNanos(writerIdleTime), MIN_TIMEOUT_NANOS);
        }

        if (allIdleTime <= 0) {
            allIdleTimeNanos = 0;
        } else {
            allIdleTimeNanos = Math.max(unit.toNanos(allIdleTime), MIN_TIMEOUT_NANOS);
        }

    }

    @Override
    public void channelRegister(AbstractGameChannelHandlerContext ctx, long playerId, GameChannelPromise promise) {
        initialize(ctx);
        ctx.fireChannelRegistered(playerId, promise);
    }

    /**
     * 初始化
     * @param ctx ctx
     * @return void
     * @date 2024/6/27 14:05
     */
    private void initialize(AbstractGameChannelHandlerContext ctx) {
        switch (state) {
            case 1:
            case 2:
                return;
        }

        state = 1;
        lastReadTime = lastWriteTime = ticksInNanos();
        // 初始化创建读取消息事件检测延时任务
        if (readerIdleTimeNanos > 0) {
            readerIdleTimeoutScheduled = schedule(ctx, new ReaderIdleTimeoutTask(ctx), readerIdleTimeNanos, TimeUnit.NANOSECONDS);
        }

        // 初始化创建写出消息事件检测延时任务
        if (writerIdleTimeNanos > 0) {
            writerIdleTimeoutScheduled = schedule(ctx, new WriterIdleTimeoutTask(ctx), writerIdleTimeNanos, TimeUnit.NANOSECONDS);
        }

        // 初始化创建读取和写出消息事件检测延时任务
        if (allIdleTimeNanos > 0) {
            allIdleTimeoutScheduled = schedule(ctx, new AllIdleTimeoutTask(ctx), allIdleTimeNanos, TimeUnit.NANOSECONDS);
        }
    }

    /**
     * 创建延时任务
     * @param ctx ctx
     * @param task task
     * @param delay delay
     * @param unit unit
     * @return java.util.concurrent.ScheduledFuture<?>
     * @date 2024/6/19 19:30
     */
    ScheduledFuture<?> schedule(AbstractGameChannelHandlerContext ctx, Runnable task, long delay, TimeUnit unit) {
        return ctx.executor().schedule(task, delay, unit);
    }

    @Override
    public void channelInactive(AbstractGameChannelHandlerContext ctx) throws Exception {
        destroy();
        ctx.fireChannelInactive();
    }

    /**
     * 销毁定时事件任务
     * @return void
     * @date 2024/6/19 19:30
     */
    private void destroy() {
        state = 2;
        if (readerIdleTimeoutScheduled != null) {
            readerIdleTimeoutScheduled.cancel(false);
            readerIdleTimeoutScheduled = null;
        }

        if (writerIdleTimeoutScheduled != null) {
            writerIdleTimeoutScheduled.cancel(false);
            writerIdleTimeoutScheduled = null;
        }

        if (allIdleTimeoutScheduled != null) {
            allIdleTimeoutScheduled.cancel(false);
            allIdleTimeoutScheduled = null;
        }
    }

    @Override
    public void channelRead(AbstractGameChannelHandlerContext ctx, Object msg) throws Exception {
        if (readerIdleTimeNanos > 0 || allIdleTimeNanos > 0) {
            // 记录最后一次读取操作的时间
            this.lastReadTime = this.ticksInNanos();
        }

        ctx.fireChannelRead(msg);
    }

    /**
     * 获取当前时间的纳秒
     * @return long
     * @date 2024/6/19 19:31
     */
    long ticksInNanos() {
        return System.nanoTime();
    }

    @Override
    public void exceptionCaught(AbstractGameChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.fireExceptionCaught(cause);
    }

    @Override
    public void writeAndFlush(AbstractGameChannelHandlerContext ctx, ResponseResult msg, GameChannelPromise promise) throws Exception {
        if (writerIdleTimeNanos > 0 || allIdleTimeNanos > 0) {
            this.lastWriteTime = this.ticksInNanos();
        }

        ctx.writeAndFlush(msg, promise);
    }

    @Override
    public void userEventTriggered(AbstractGameChannelHandlerContext ctx, Object evt, Promise<Object> promise) throws Exception {
        ctx.fireUserEventTriggered(evt, promise);
    }

    @Override
    public void channelReadRpcRequest(AbstractGameChannelHandlerContext ctx, GameMessagePackage msg) throws Exception {
        ctx.fireChannelReadRPCRequest(msg);
    }

    @Override
    public void writeRPCMessage(AbstractGameChannelHandlerContext ctx, GameMessagePackage gameMessage,
                                Promise<GameMessagePackage> callback) {
        ctx.writeRPCMessage(gameMessage, callback);
    }

    @Override
    public void close(AbstractGameChannelHandlerContext ctx, GameChannelPromise promise) {
        ctx.close(promise);
    }



    /**
     * 公共抽象任务
     * @date 2024/6/19 19:31
     */
    private abstract static class AbstractIdleTask implements Runnable {
        /**  */
        private final AbstractGameChannelHandlerContext ctx;

        AbstractIdleTask(AbstractGameChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void run() {
            if (!ctx.gameChannel().isRegistered()) {
                return;
            }

            run(ctx);
        }

        /**
         * 运行延时任务
         * @param ctx ctx
         * @return void
         * @date 2024/6/27 14:07
         */
        protected abstract void run(AbstractGameChannelHandlerContext ctx);

    }

    /**
     * 读取消息检测任务
     * 8.3.8 GameChannel 空闲超时处理 p230 p233
     * @date 2024/6/19 19:31
     */
    private final class ReaderIdleTimeoutTask extends AbstractIdleTask {
        ReaderIdleTimeoutTask(AbstractGameChannelHandlerContext ctx) {
            super(ctx);
        }

        @Override
        protected void run(AbstractGameChannelHandlerContext ctx) {
            long nextDelay = readerIdleTimeNanos;
            nextDelay -= ticksInNanos() - lastReadTime;
            // 说明读取事件超时，发送空闲事件，并创建新的延迟任务，用于下次超时检测
            if (nextDelay <= 0) {
                readerIdleTimeoutScheduled = schedule(ctx, this, readerIdleTimeNanos, TimeUnit.NANOSECONDS);
                try {
                    IdleStateEvent event = newIdleStateEvent(IdleState.READER_IDLE);
                    channelIdle(ctx, event);
                } catch (Throwable t) {
                    ctx.fireExceptionCaught(t);
                }
            } else {
                // 没有超时，从上次读取的时间起，计时计算下次超时检测
                readerIdleTimeoutScheduled = schedule(ctx, this, nextDelay, TimeUnit.NANOSECONDS);
            }
        }
    }

    /**
     * 写入消息检测任务
     * @date 2024/6/19 19:31
     */
    private final class WriterIdleTimeoutTask extends AbstractIdleTask {

        WriterIdleTimeoutTask(AbstractGameChannelHandlerContext ctx) {
            super(ctx);
        }

        @Override
        protected void run(AbstractGameChannelHandlerContext ctx) {

            long lastWriteTime = IdleStateGameChannelHandler.this.lastWriteTime;
            long nextDelay = writerIdleTimeNanos - (ticksInNanos() - lastWriteTime);
            if (nextDelay <= 0) {
                // Writer is idle - set a new timeout and notify the callback.
                writerIdleTimeoutScheduled = schedule(ctx, this, writerIdleTimeNanos, TimeUnit.NANOSECONDS);

                try {
                    IdleStateEvent event = newIdleStateEvent(IdleState.WRITER_IDLE);
                    channelIdle(ctx, event);
                } catch (Throwable t) {
                    ctx.fireExceptionCaught(t);
                }
            } else {
                writerIdleTimeoutScheduled = schedule(ctx, this, nextDelay, TimeUnit.NANOSECONDS);
            }
        }
    }

    private final class AllIdleTimeoutTask extends AbstractIdleTask {

        AllIdleTimeoutTask(AbstractGameChannelHandlerContext ctx) {
            super(ctx);
        }

        @Override
        protected void run(AbstractGameChannelHandlerContext ctx) {
            long nextDelay = allIdleTimeNanos;
            nextDelay -= ticksInNanos() - Math.max(lastReadTime, lastWriteTime);
            if (nextDelay <= 0) {
                // Both reader and writer are idle - set a new timeout and
                // notify the callback.
                allIdleTimeoutScheduled = schedule(ctx, this, allIdleTimeNanos, TimeUnit.NANOSECONDS);
                try {
                    IdleStateEvent event = newIdleStateEvent(IdleState.ALL_IDLE);
                    channelIdle(ctx, event);
                } catch (Throwable t) {
                    ctx.fireExceptionCaught(t);
                }
            } else {
                allIdleTimeoutScheduled = schedule(ctx, this, nextDelay, TimeUnit.NANOSECONDS);
            }
        }
    }

    /**
     * 获取空闲事件类型
     * 8.3.8 GameChannel 空闲超时处理 p229
     * @param state state
     * @return io.netty.handler.timeout.IdleStateEvent
     * @date 2024/6/19 19:31
     */
    protected IdleStateEvent newIdleStateEvent(IdleState state) {
        switch (state) {
            case ALL_IDLE:
                return IdleStateEvent.ALL_IDLE_STATE_EVENT;
            case READER_IDLE:
                return IdleStateEvent.READER_IDLE_STATE_EVENT;
            case WRITER_IDLE:
                return IdleStateEvent.WRITER_IDLE_STATE_EVENT;
            default:
                throw new IllegalArgumentException("Unhandled: state=" + state);
        }
    }

    /**
     * 发送空闲事件
     * @param ctx ctx
     * @param evt evt
     * @return void
     * @date 2024/6/19 19:31
     */
    protected void channelIdle(AbstractGameChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        ctx.fireUserEventTriggered(evt, null);
    }
}
