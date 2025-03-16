package com.mumu.framework.core.cloud;

import org.springframework.context.ApplicationEvent;

/**
 * GameChannelCloseEvent
 *
 * @author liuzhen
 * @version 1.0.0 2025/3/3 23:43
 */
public class GameChannelCloseEvent extends ApplicationEvent {
    private long playerId;

    public GameChannelCloseEvent(Object source, long playerId) {
        super(source);
        this.playerId = playerId;
    }

    public long getPlayerId() {
        return playerId;
    }
}
