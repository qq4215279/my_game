/*
 * Copyright 2020-2026, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.game.core.game_netty.context;

import com.mumu.game.core.cmd.response.ResponseResult2;
import com.mumu.game.core.game_netty.channel.context.AbstractGameChannelHandlerContext;
import com.mumu.game.core.net.server.MessageContext;
import com.mumu.game.proto.message.system.message.GameMessageHeader;
import com.mumu.game.proto.message.system.message.GameMessagePackage;

import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import lombok.Getter;

/**
 * GameMessageContextImpl
 *
 * @author liuzhen
 * @version 1.0.0 2025/3/30 17:44
 */
@Getter
public class GameMessageContextImpl implements GameMessageContext {
    private long playerId;
    private MessageContext context;
    private GameMessagePackage reqGameMessagePackage;
    
    private AbstractGameChannelHandlerContext gameContext;


    /**
     * 构造方法
     * @param context context
     * @param gameChannelHandlerContext ctx
     */
    public GameMessageContextImpl(MessageContext context, AbstractGameChannelHandlerContext gameChannelHandlerContext) {
        GameMessagePackage gameMessagePackage = context.getMessagePackage();
        this.context = context;
        this.playerId = gameMessagePackage.getHeader().getPlayerId();

        this.reqGameMessagePackage = gameMessagePackage;
        this.gameContext = gameChannelHandlerContext;
    }

    /**
     * 将同一条消息广播给本服的所有人
     * @param message message
     * @return void
     */
    public void broadcastMessage(ResponseResult2 message) {
        if (message != null) {
            gameContext.gameChannel().getGameMessageDispatchServlet().broadcastMessage(message);
        }
    }

    public void broadcastMessage(ResponseResult2 message, long... playerIds) {
        gameContext.gameChannel().getGameMessageDispatchServlet().broadcastMessage(message, playerIds);
    }


    /**
     * 向某个playerId的GameChannel中发送一个事件
     * @param event event
     * @param promise promise
     * @param playerId playerId
     * @return io.netty.util.concurrent.Future<java.lang.Object>
     * @author liuzhen
     * @since 2025/3/30 17:53
     */
    public Future<Object> sendUserEvent(Object event, Promise<Object> promise, long playerId) {
        gameContext.gameChannel().getGameMessageDispatchServlet().fireUserEvent(playerId, event, promise);
        return promise;
    }

    public <E> DefaultPromise<E> newPromise() {
        return new DefaultPromise<>(gameContext.executor());
    }

    public DefaultPromise<GameMessagePackage> newRPCPromise() {
        return new DefaultPromise<>(gameContext.executor());
    }

    @Override
    public long getPlayerId() {
        return this.playerId;
    }

    @Override
    public String getRemoteHost() {
        // TODO
        // return this.reqGameMessagePackage.getHeader().getAttribute().getClientIp();
        return "";
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> E getRequest() {
        return (E)this.reqGameMessagePackage;
        // TODO
    }

    @Override
    public void sendMessage(ResponseResult2 response) {
        if (response != null) {
            wrapResponseMessage(response);
            gameContext.writeAndFlush(response);
        }
    }

    private void wrapResponseMessage(ResponseResult2 response) {
        GameMessageHeader responseHeader = response.getHeader();
        GameMessageHeader requestHeader = this.reqGameMessagePackage.getHeader();
        responseHeader.setSendTime(requestHeader.getSendTime());
        responseHeader.setSeq(requestHeader.getSeq());
        responseHeader.setPlayerId(requestHeader.getPlayerId());
        responseHeader.setSendTime(System.currentTimeMillis());
        responseHeader.setVersion(requestHeader.getVersion());
    }

}
