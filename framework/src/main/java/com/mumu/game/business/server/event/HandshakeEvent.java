package com.mumu.game.business.server.event;

import org.springframework.context.ApplicationEvent;

import com.mumu.game.proto.message.server.ClientServerBean;

import lombok.Getter;

/**
 * HandshakeEvent
 * 握手事件
 * @author liuzhen
 * @version 1.0.0 2026/5/5 16:15
 */
public class HandshakeEvent extends ApplicationEvent {

    /** true-本服作为服务端触发的握手事件 */
    @Getter
    private final boolean server;

    public HandshakeEvent(Object source, boolean server) {
        super(source);
        this.server = server;
    }

    public ClientServerBean getClientServer() {
        return (ClientServerBean) getSource();
    }

    public static HandshakeEvent of(ClientServerBean clientServer, boolean isServer) {
        return new HandshakeEvent(clientServer, isServer);
    }
}