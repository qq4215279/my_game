/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.game_netty.channel.context;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;

import com.mumu.common.proto.message.system.message.GameMessageHeader;
import com.mumu.common.proto.message.system.message.GameMessagePackage;
import com.mumu.common.thread.GameEventExecutorGroup;
import com.mumu.framework.core.cmd.response.ResponseResult;
import com.mumu.framework.core.game_netty.channel.GameChannel;
import com.mumu.framework.core.game_netty.channel.future.DefaultGameChannelPromise;
import com.mumu.framework.core.game_netty.channel.future.GameChannelFuture;
import com.mumu.framework.core.game_netty.channel.future.GameChannelPromise;
import com.mumu.framework.core.game_netty.channel.handler.GameChannelHandler;
import com.mumu.framework.core.game_netty.channel.handler.GameChannelInboundHandler;
import com.mumu.framework.core.game_netty.channel.handler.GameChannelOutboundHandler;
import com.mumu.framework.util.JProtoBufUtil;

import io.netty.channel.DefaultChannelPipeline;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * GameChannelPipeline
 *
 * @author liuzhen
 * @version 1.0.0 2025/3/30 16:50
 */
public class GameChannelPipeline {
    static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultChannelPipeline.class);

    /** headName */
    private static final String HEAD_NAME = generateName0(HeadChannelHandlerContext.class);
    /** tailName */
    private static final String TAIL_NAME = generateName0(TailChannelHandlerContext.class);

    /**  */
    private final GameChannel channel;
    /** 头context */
    private final AbstractGameChannelHandlerContext head;
    /** 尾context */
    private final AbstractGameChannelHandlerContext tail;

    /**  */
    private Map<EventExecutorGroup, EventExecutor> childExecutors;

    /**  */
    private static final FastThreadLocal<Map<Class<?>, String>> nameCaches =
        new FastThreadLocal<Map<Class<?>, String>>() {
            @Override
            protected Map<Class<?>, String> initialValue() throws Exception {
                return new WeakHashMap<Class<?>, String>();
            }
        };

    public GameChannelPipeline(GameChannel channel) {
        this.channel = ObjectUtil.checkNotNull(channel, "channel");

        tail = new TailChannelHandlerContext(this);
        head = new HeadChannelHandlerContext(this);

        head.next = tail;
        tail.prev = head;
    }

    public final GameChannel gameChannel() {
        return channel;
    }

    public final GameChannelPipeline addFirst(String name, boolean singleEventExecutorPerGroup,
        GameChannelHandler handler) {
        return addFirst(null, singleEventExecutorPerGroup, name, handler);
    }

    public final GameChannelPipeline addFirst(GameChannelHandler... handlers) {
        return addFirst(null, false, handlers);
    }

    public final GameChannelPipeline addFirst(GameEventExecutorGroup executor, boolean singleEventExecutorPerGroup,
        GameChannelHandler... handlers) {
        if (handlers == null) {
            throw new NullPointerException("handlers");
        }
        if (handlers.length == 0 || handlers[0] == null) {
            return this;
        }

        int size;
        for (size = 1; size < handlers.length; size++) {
            if (handlers[size] == null) {
                break;
            }
        }

        for (int i = size - 1; i >= 0; i--) {
            GameChannelHandler h = handlers[i];
            addFirst(executor, singleEventExecutorPerGroup, null, h);
        }

        return this;
    }

    public final GameChannelPipeline addFirst(GameEventExecutorGroup group, boolean singleEventExecutorPerGroup,
        String name, GameChannelHandler handler) {
        final AbstractGameChannelHandlerContext newCtx;
        synchronized (this) {
            name = filterName(name, handler);
            newCtx = newContext(group, singleEventExecutorPerGroup, name, handler);
            addFirst0(newCtx);
        }
        return this;
    }

    private void addFirst0(AbstractGameChannelHandlerContext newCtx) {
        AbstractGameChannelHandlerContext nextCtx = head.next;
        newCtx.prev = head;
        newCtx.next = nextCtx;
        head.next = newCtx;
        nextCtx.prev = newCtx;
    }

    /**
     * TODO
     *
     * @param handlers handlers
     * @return com.mygame.gateway.message.channel.context.GameChannelPipeline
     * @date 2024/6/26 20:02
     */
    public final GameChannelPipeline addLast(GameChannelHandler... handlers) {
        return addLast(null, false, handlers);
    }

    public final GameChannelPipeline addLast(GameEventExecutorGroup executor, boolean singleEventExecutorPerGroup,
        GameChannelHandler... handlers) {
        if (handlers == null) {
            throw new NullPointerException("handlers");
        }

        for (GameChannelHandler h : handlers) {
            if (h == null) {
                break;
            }
            addLast(executor, false, null, h);
        }

        return this;
    }

    public final GameChannelPipeline addLast(boolean singleEventExecutorPerGroup, String name,
        GameChannelHandler handler) {
        return addLast(null, singleEventExecutorPerGroup, name, handler);
    }

    public final GameChannelPipeline addLast(GameEventExecutorGroup group, boolean singleEventExecutorPerGroup,
        String name, GameChannelHandler handler) {
        final AbstractGameChannelHandlerContext newCtx;
        synchronized (this) {
            newCtx = newContext(group, singleEventExecutorPerGroup, filterName(name, handler), handler);
            addLast0(newCtx);
        }

        return this;
    }

    private void addLast0(AbstractGameChannelHandlerContext newCtx) {
        AbstractGameChannelHandlerContext prev = tail.prev;
        newCtx.prev = prev;
        newCtx.next = tail;
        prev.next = newCtx;
        tail.prev = newCtx;
    }

    /**
     * Description: 创建一个实例
     *
     * @param group
     * @param singleEventExecutorPerGroup
     *            如果为true，那么多个不同的Handler如果使用同一个GameEventExecutorGroup中选择EventExecutor，在调用handler里面的方法时，都是使用的同一个EventExecutor;
     * @param name
     * @param handler
     * @return
     * @author wgs
     * @date 2019年5月25日 下午6:42:49
     */
    private AbstractGameChannelHandlerContext newContext(GameEventExecutorGroup group,
        boolean singleEventExecutorPerGroup, String name, GameChannelHandler handler) {
        return new DefaultGameChannelHandlerContext(this, childExecutor(group, singleEventExecutorPerGroup), name,
            handler);
    }

    private EventExecutor childExecutor(GameEventExecutorGroup group, boolean singleEventExecutorPerGroup) {
        if (group == null) {
            return null;
        }

        if (!singleEventExecutorPerGroup) {
            return group.next();
        }

        Map<EventExecutorGroup, EventExecutor> childExecutors = this.childExecutors;
        if (childExecutors == null) {
            // Use size of 4 as most people only use one extra EventExecutor.
            childExecutors = this.childExecutors = new IdentityHashMap<EventExecutorGroup, EventExecutor>(4);
        }

        // Pin one of the child executors once and remember it so that the same child executor
        // is used to fire events for the same channel.
        EventExecutor childExecutor = childExecutors.get(group);
        if (childExecutor == null) {
            childExecutor = group.next();
            childExecutors.put(group, childExecutor);
        }

        return childExecutor;
    }

    private String filterName(String name, GameChannelHandler handler) {
        if (name == null) {
            return generateName(handler);
        }
        checkDuplicateName(name);
        return name;
    }

    private String generateName(GameChannelHandler handler) {
        Map<Class<?>, String> cache = nameCaches.get();
        Class<?> handlerType = handler.getClass();
        String name = cache.get(handlerType);
        if (name == null) {
            name = generateName0(handlerType);
            cache.put(handlerType, name);
        }

        if (context0(name) != null) {
            String baseName = name.substring(0, name.length() - 1); // Strip the trailing '0'.
            for (int i = 1;; i++) {
                String newName = baseName + i;
                if (context0(newName) == null) {
                    name = newName;
                    break;
                }
            }
        }
        return name;
    }

    private static String generateName0(Class<?> handlerType) {
        return StringUtil.simpleClassName(handlerType) + "#0";
    }

    private void checkDuplicateName(String name) {
        if (context0(name) != null) {
            throw new IllegalArgumentException("Duplicate handler name: " + name);
        }
    }

    private AbstractGameChannelHandlerContext context0(String name) {
        AbstractGameChannelHandlerContext context = head.next;
        while (context != tail) {
            if (context.name().equals(name)) {
                return context;
            }
            context = context.next;
        }
        return null;
    }



    // ================================================================================================================>

    public final GameChannelPipeline fireRegister(long playerId, GameChannelPromise promise) {
        AbstractGameChannelHandlerContext.invokeChannelRegistered(head, playerId, promise);
        return this;
    }

    public final GameChannelPipeline fireChannelInactive() {
        AbstractGameChannelHandlerContext.invokeChannelInactive(head);
        return this;
    }

    public final GameChannelPipeline fireChannelRead(Object msg) {
        AbstractGameChannelHandlerContext.invokeChannelRead(head, msg);
        return this;
    }

    public final GameChannelPipeline fireExceptionCaught(Throwable cause) {
        AbstractGameChannelHandlerContext.invokeExceptionCaught(head, cause);
        return this;
    }

    public final GameChannelPipeline fireUserEventTriggered(Object event, Promise<Object> promise) {
        AbstractGameChannelHandlerContext.invokeUserEventTriggered(head, event, promise);
        return this;
    }

    public final GameChannelPipeline fireChannelReadRPCRequest(GameMessagePackage gameMessage) {
        AbstractGameChannelHandlerContext.invokeChannelReadRPCRequest(head, gameMessage);
        return this;
    }


    public final GameChannelFuture writeAndFlush(ResponseResult msg) {
        return tail.writeAndFlush(msg);
    }

    public final GameChannelFuture writeAndFlush(ResponseResult msg, GameChannelPromise promise) {
        return tail.writeAndFlush(msg, promise);
    }

    public final void writeRpcRequest(GameMessagePackage gameMessage, Promise<GameMessagePackage> promise) {
        tail.writeRPCMessage(gameMessage, promise);
    }

    public final GameChannelFuture close() {
        return tail.close(new DefaultGameChannelPromise(this.channel));
    }

    protected void onUnhandledInboundException(Throwable cause) {
        try {
            logger.warn("An exceptionCaught() event was fired, and it reached at the tail of the pipeline. "
                + "It usually means the last handler in the pipeline did not handle the exception.", cause);
        } finally {
            ReferenceCountUtil.release(cause);
        }
    }

    protected void onUnhandledInboundMessage(Object msg) {
        try {
            logger.debug("Discarded inbound message {} that reached at the tail of the pipeline. "
                + "Please check your pipeline configuration.", msg);
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    // ================================================================================================================>


    // A special catch-all handler that handles both bytes and messages.
    /**
     * 尾ChannelHandlerContext
     * 
     * @date 2024/6/26 13:58
     */
    final class TailChannelHandlerContext extends AbstractGameChannelHandlerContext
        implements GameChannelInboundHandler {

        TailChannelHandlerContext(GameChannelPipeline pipeline) {
            super(pipeline, null, TAIL_NAME, true, false);

        }

        @Override
        public GameChannelHandler handler() {
            return this;
        }

        @Override
        public void channelRegister(AbstractGameChannelHandlerContext ctx, long playerId, GameChannelPromise promise) {
            promise.setSuccess();
            logger.debug("注册事件未处理");
        }

        @Override
        public void channelInactive(AbstractGameChannelHandlerContext ctx) throws Exception {

        }

        @Override
        public void channelRead(AbstractGameChannelHandlerContext ctx, Object msg) throws Exception {
            onUnhandledInboundMessage(msg);
        }

        @Override
        public void exceptionCaught(AbstractGameChannelHandlerContext ctx, Throwable cause) throws Exception {
            onUnhandledInboundException(cause);
        }

        @Override
        public void userEventTriggered(AbstractGameChannelHandlerContext ctx, Object evt, Promise<Object> promise) {

        }

        @Override
        public void channelReadRpcRequest(AbstractGameChannelHandlerContext ctx, GameMessagePackage msg)
            throws Exception {
            onUnhandledInboundMessage(msg);
        }
    }

    /**
     * 头ChannelHandlerContext
     * 
     * @date 2024/6/26 13:58
     */
    final class HeadChannelHandlerContext extends AbstractGameChannelHandlerContext
        implements GameChannelOutboundHandler, GameChannelInboundHandler {

        HeadChannelHandlerContext(GameChannelPipeline pipeline) {
            super(pipeline, null, HEAD_NAME, false, true);
        }

        @Override
        public GameChannelHandler handler() {
            return this;
        }

        @Override
        public void channelRegister(AbstractGameChannelHandlerContext ctx, long playerId, GameChannelPromise promise) {
            ctx.fireChannelRegistered(playerId, promise);
        }

        @Override
        public void channelInactive(AbstractGameChannelHandlerContext ctx) throws Exception {
            ctx.fireChannelInactive();
        }

        @Override
        public void channelRead(AbstractGameChannelHandlerContext ctx, Object msg) throws Exception {
            ctx.fireChannelRead(msg);
        }

        @Override
        public void exceptionCaught(AbstractGameChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.fireExceptionCaught(cause);
        }

        @Override
        public void userEventTriggered(AbstractGameChannelHandlerContext ctx, Object evt, Promise<Object> promise)
            throws Exception {
            ctx.fireUserEventTriggered(evt, promise);
        }

        @Override
        public void channelReadRpcRequest(AbstractGameChannelHandlerContext ctx, GameMessagePackage msg)
                throws Exception {
            ctx.fireChannelReadRPCRequest(msg);
        }

        @Override
        public void writeAndFlush(AbstractGameChannelHandlerContext ctx, ResponseResult responseResult,
                                  GameChannelPromise promise) throws Exception {
            GameMessagePackage gameMessagePackage = new GameMessagePackage();

            GameMessageHeader header = cn.hutool.core.util.ObjectUtil.cloneByStream(responseResult.getHeader());
            // 重新设置playerId，防止不同channel之间由于使用同一个GameMessagePackage实例，相互覆盖
            header.setPlayerId(pipeline.channel.getPlayerId());
            header.setErrorCode(responseResult.getErrorCode());
            header.setMessageId(responseResult.getCmd().getResMessageId());
            header.setToServerId(channel.getGatewayServerId());
            header.setFromServerId(channel.getServerConfig().getServerId());
            header.setServerSendTime(System.currentTimeMillis());
            // 设置头信息
            gameMessagePackage.setHeader(header);
            // 返回信息
            gameMessagePackage.setBody(JProtoBufUtil.encode(responseResult.getResMsg()));
            // 调用GameChannel的方法，向外部发送消息
            channel.unsafeSendMessage(gameMessagePackage, promise);
        }

        @Override
        public void writeRPCMessage(AbstractGameChannelHandlerContext ctx, GameMessagePackage gameMessage,
            Promise<GameMessagePackage> callback) {
            channel.unsafeSendRpcMessage(gameMessage, callback);
        }

        @Override
        public void close(AbstractGameChannelHandlerContext ctx, GameChannelPromise promise) {
            channel.unsafeClose();
        }

    }
}
