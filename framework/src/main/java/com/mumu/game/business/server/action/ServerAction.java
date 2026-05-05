package com.mumu.game.business.server.action;

import com.mumu.game.business.server.event.HandshakeEvent;
import com.mumu.game.core.cmd.anno.CmdAction;
import com.mumu.game.core.cmd.anno.RpcCmdMapping;
import com.mumu.game.core.cmd.enums.RpcCmd;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.core.net.server.IoSession;
import com.mumu.game.core.net.server.MessageContext;
import com.mumu.game.core.net.session.SessionManager;
import com.mumu.game.core.properties.ServerInfo;
import com.mumu.game.core.utils.SpringContextUtils;
import com.mumu.game.proto.message.server.ClientServerBean;
import com.mumu.game.proto.message.server.ReconnectServerMsgEA;

import jakarta.annotation.Resource;

/**
 * ServerAction
 * 服务器相关消息处理
 * @author liuzhen
 * @version 1.0.0 2026/5/5 16:14
 */
@CmdAction
public class ServerAction {

    @Resource
    ServerInfo serverInfo;
    @Resource
    SessionManager sessionManager;

    /** 收到其他服务的握手请求 */
    @RpcCmdMapping(RpcCmd.ServerInfoHandshake)
    public void reqHandshake(MessageContext context) {
        ReconnectServerMsgEA ea = context.getMsg(ReconnectServerMsgEA.class);
        ClientServerBean clientServer = ea.getClientServerBean();
        LogTopic.NET.info("reqHandshake", "clientServer", clientServer);

        IoSession session = context.getSession();
        // 响应握手请求，返回当前服务器信息
        // MessageSender.send(session, Cmd.ResServerInfoHandshake, serverInfo.build());
        // 缓存服务器信息
        sessionManager.addServerSession(session, clientServer);
        // 发布服务握手事件
        SpringContextUtils.publishEvent(HandshakeEvent.of(clientServer, true));
    }
}
