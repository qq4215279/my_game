/*
 * Copyright 2020-2025, mumu without 996.
 * All Right Reserved.
 */

package com.mumu.framework.core.mvc.servlet;

import com.mumu.common.proto.message.system.message.GameMessagePackage;
import com.mumu.common.utils.JWTUtil;
import com.mumu.framework.core.mvc.servlet.handler.ConfirmHandler;

import io.netty.channel.ChannelHandlerContext;
import lombok.Data;

/**
 * DispatchServlet
 *
 * @author liuzhen
 * @version 1.0.0 2025/2/24 23:06
 */
@Data
public class DispatchServlet implements Servlet {
    /**  */
    private JWTUtil.TokenBody tokenBody;

    @Override
    public void init() {

    }

    @Override
    public void doCommand(ChannelHandlerContext ctx, Object msg) {
        GameMessagePackage gameMessagePackage = (GameMessagePackage)msg;
        int serviceId = gameMessagePackage.getHeader().getServiceId();

        Request request = new Request(ctx.channel(), ctx, gameMessagePackage);
        Response response = new Response(ctx.channel());

        // 如果首次通信，获取验证信息
        if (tokenBody == null) {
            ConfirmHandler confirmHandler = (ConfirmHandler)request.getChannel().pipeline().get("ConfirmHandler");
            tokenBody = confirmHandler.getTokenBody();
        }


    }

    /* private static void dispatchMessage(EventExecutor executor,
                                        PlayerServiceManager playerServiceInstance, long playerId, int serviceId,
                                        String clientIp,
                                        GameMessagePackage gameMessagePackage, GatewayServerConfig gatewayServerConfig) {

        Promise<Integer> promise = new DefaultPromise<>(executor);
        playerServiceInstance.selectServerId(playerId, serviceId, promise)
                .addListener(new GenericFutureListener<Future<Integer>>() {

                    @Override
                    public void operationComplete(Future<Integer> future) throws Exception {
                        if (future.isSuccess()) {
                            Integer toServerId = future.get();
                            gameMessagePackage.getHeader().setToServerId(toServerId);
                            gameMessagePackage.getHeader().setFromServerId(gatewayServerConfig.getServerId());
                            gameMessagePackage.getHeader().getAttribute().setClientIp(clientIp);
                            gameMessagePackage.getHeader().setPlayerId(playerId);

                            // 动态创建与业务服务交互的消息总线Topic
                            String topic = TopicUtil.generateTopic(gatewayServerConfig.getBusinessGameMessageTopic(), toServerId);
                            // 向消息总线服务发布客户端请求消息。
                            byte[] value = GameMessageInnerDecoder.sendMessage(gameMessagePackage);
                            ProducerRecord<String, byte[]> record = new ProducerRecord<String, byte[]>(topic, String.valueOf(playerId), value);
                            kafkaTemplate.send(record);

                            logger.debug("发送到{}消息成功->{}", gameMessagePackage.getHeader());

                        } else {
                            logger.error("消息发送失败", future.cause());
                        }
                    }
                });
    } */

    @Override
    public void destroy() {

    }
}
