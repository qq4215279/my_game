/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.mvc.servlet.handler;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.mumu.common.proto.message.core.ErrorCode;
import com.mumu.common.proto.message.gate.core.ConnectConfirmMsgCE;
import com.mumu.common.proto.message.gate.core.ConnectConfirmMsgEC;
import com.mumu.common.proto.message.system.message.GameMessageHeader;
import com.mumu.common.proto.message.system.message.GameMessagePackage;
import com.mumu.common.utils.AESUtils;
import com.mumu.common.utils.Base64Utils;
import com.mumu.common.utils.JWTUtil;
import com.mumu.common.utils.RSAUtils;
import com.mumu.framework.core.cloud.IoSession;
import com.mumu.framework.core.cloud.PlayerServiceManager;
import com.mumu.framework.core.cmd.enums.Cmd;
import com.mumu.framework.core.log.LogTopic;
import com.mumu.framework.core.mvc.GatewayServerConfig;
import com.mumu.framework.core.mvc.servlet.handler.codec.JProtobufDecoder;
import com.mumu.framework.core.mvc.servlet.handler.codec.JProtobufEncoder;
import com.mumu.framework.core.mvc.servlet.session.PlayerSessionManager;
import com.mumu.framework.util.JProtoBufUtil;

import io.jsonwebtoken.ExpiredJwtException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.Getter;

/**
 * ConfirmHandler
 * 连接验证handler
 * @author liuzhen
 * @version 1.0.0 2025/2/24 23:28
 */
public class ConfirmHandler extends ChannelInboundHandlerAdapter {
    private static final LogTopic log = LogTopic.NET;

    /** 注入服务端配置 */
    private GatewayServerConfig serverConfig;
    /** 注入业务服务管理类，从这里获取负载均衡的服务器信息 */
    private PlayerServiceManager businessServerService;
    /** session管理器 */
    private PlayerSessionManager sessionManager;

    /** 标记连接是否认证成功 */
    private boolean confirmSuccess = false;
    /** 定时器的返回值 */
    private ScheduledFuture<?> future;
    /**  */
    @Getter
    private JWTUtil.TokenBody tokenBody;


    public ConfirmHandler(GatewayServerConfig serverConfig) {
        this.serverConfig = serverConfig;
        this.sessionManager = PlayerSessionManager.self();
        this.businessServerService = PlayerServiceManager.self();
    }

    /**
     * 此方法会在连接建立成功channel注册之后调用
     * @param ctx ctx
     * @return void
     * @date 2024/6/19 17:37
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("客户端连接成功", "clientIp", IoSession.getRemoteIP(ctx.channel()), "channelId", ctx.channel().id().asShortText());

        // 从配置中获取延迟时间
        int delay = serverConfig.getWaiteConfirmTimeoutSecond();
        future = ctx.channel().eventLoop().schedule(() -> {
            // 如果没有认证成功，则关闭连接。
            if (!confirmSuccess) {
                log.info("连接认证超时，断开连接", "channelId", ctx.channel().id().asShortText());
                ctx.close();
            }
        }, delay, TimeUnit.SECONDS);

        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // 如果连接关闭了，取消息定时检测任务。
        if (future != null) {
            future.cancel(true);
        }

        // 连接断开之后，移除连接
        if (tokenBody != null) {
            long playerId = tokenBody.getPlayerId();
            // 调用移除，否则出现内存泄漏的问题。
            this.sessionManager.removeChannel(playerId, ctx.channel());
        }

        // 接着告诉下面的Handler
        ctx.fireChannelInactive();
    }

    // p156
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        GameMessagePackage gameMessagePackage = (GameMessagePackage)msg;
        int messageId = gameMessagePackage.getHeader().getMessageId();

        // 如果是认证消息，在这里处理
        if (messageId == Cmd.ConnectConfirmMsg.getReqMessageId()) {
            ConnectConfirmMsgCE reqMsg = JProtoBufUtil.decode(gameMessagePackage.getBody(), ConnectConfirmMsgCE.class);
            // 反序列化消息内容
            String token = reqMsg.getToken();

            ConnectConfirmMsgEC resMsg = new ConnectConfirmMsgEC();
            GameMessageHeader header = Cmd.ConnectConfirmMsg.buildGameMessageHeader(false);

            // 检测token
            if (StringUtils.isEmpty(token)) {
                log.error("token为空，直接关闭连接");
                ctx.close();

            } else {
                try {
                    // 解析token里面的内容，如果解析失败，会抛出异常
                    tokenBody = JWTUtil.getTokenBody(token);
                    // 标记认证成功
                    this.confirmSuccess = true;

                    // 检测重复连接
                    this.repeatedConnect();

                    IoSession ioSession = IoSession.of(ctx.channel());
                    // 加入连接管理
                    sessionManager.addChannel(tokenBody.getPlayerId(), ioSession);

                    // 生成此连接的AES密钥
                    String aesSecretKey = AESUtils.createSecret(tokenBody.getUserId(), tokenBody.getServerId());

                    // TODO 将对称加密密钥分别设置到编码和解码的handler中。
                    JProtobufDecoder decodeHandler = ctx.channel().pipeline().get(JProtobufDecoder.class);
                    decodeHandler.setAesSecret(aesSecretKey);
                    JProtobufEncoder encodeHandler = ctx.channel().pipeline().get(JProtobufEncoder.class);
                    encodeHandler.setAesSecret(aesSecretKey);

                    byte[] clientPublicKey = getClientRsaPublicKey();
                    // 使用客户端的公钥加密对称加密密钥
                    byte[] encryptAesKey = RSAUtils.encryptByPublicKey(aesSecretKey.getBytes(), clientPublicKey);
                    // 返回给客户端
                    resMsg.setSecretKey(Base64Utils.encodeToString(encryptAesKey));


                    GameMessagePackage returnPackage = new GameMessagePackage();
                    returnPackage.setHeader(header);
                    returnPackage.setBody(JProtoBufUtil.encode(resMsg));

                    ctx.writeAndFlush(returnPackage);

                    // TODO 通知各个服务，某个用户连接成功
                    // String ip = IoSession.getRemoteIP(ctx.channel());
                    // this.sendConnectStatusMsg(true, ctx.executor(), ip);

                } catch (Exception e) {
                    // 告诉客户端token过期，它客户端重新获取并重新连接
                    if (e instanceof ExpiredJwtException) {
                        header.setErrorCode(ErrorCode.FAIL_TOKEN_ILLEGAL);
                        ctx.writeAndFlush(resMsg);
                        ctx.close();
                        log.warn("token过期，关闭连接");
                    } else {
                        log.error(e,"token解析异常，直接关闭连接");
                        ctx.close();
                    }
                }
            }

        } else {
            if (!confirmSuccess) {
                log.info("连接未认证，不处理任务消息，关闭连接", "channelId", ctx.channel().id().asShortText());
                ctx.close();
                return;
            }

            // 如果不是认证消息，则向下发送消息，让后面的Handler去处理，如果不下发，后面的Handler将接收不到消息。
            ctx.fireChannelRead(msg);
        }
    }

    private void repeatedConnect() {
        if (tokenBody != null) {
            IoSession existIoSession = this.sessionManager.getChannel(tokenBody.getPlayerId());
            if (existIoSession != null) {
                // 如果检测到同一个账号创建了多个连接，则把旧连接关闭，保留新连接。

                GameMessageHeader header = Cmd.ConnectConfirmMsg.buildGameMessageHeader(false);
                header.setErrorCode(ErrorCode.FAIL_REPEATED_CONNECT);

                GameMessagePackage returnPackage = new GameMessagePackage();
                returnPackage.setHeader(header);
                // 在关闭之后，给这个连接返回一条提示信息，告诉客户端账号可能异地登陆了。
                existIoSession.write(returnPackage);

                existIoSession.close();
            }
        }
    }

    private void sendConnectStatusMsg(boolean connect, EventExecutor executor, String clientIp) {
        /* ConnectionStatusMsgRequest request = new ConnectionStatusMsgRequest();
        request.getBodyObj().setConnect(connect);
        long playerId = tokenBody.getPlayerId();
        Set<Integer> allServiceId = businessServerService.getAllServiceId();
        for (Integer serviceId : allServiceId) {
            // 通知所有的服务，用户的连接状态
            GameMessagePackage gameMessagePackage = new GameMessagePackage();
            gameMessagePackage.setBody(request.body());
            gameMessagePackage.setHeader(request.getHeader());
            DispatchGameMessageHandler.dispatchMessage(kafkaTemplate, executor, businessServerService, playerId,
                    serviceId, clientIp, gameMessagePackage, serverConfig);
        } */
    }

    /**
     * 从token中获取客户端的公钥
     * @return byte[]
     * @date 2024/6/25 16:57
     */
    private byte[] getClientRsaPublicKey() {
        // 获取客户端的公钥字符串。
        String publicKey = tokenBody.getParam()[1];
        return Base64Utils.decodeFromString(publicKey);
    }

}
