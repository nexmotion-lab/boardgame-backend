package com.coders.boardgame.domain.game.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PlayerDisconnectedEvent extends ApplicationEvent {
    private final String roomId;
    private final Long playerId;
    private final boolean isUnexpected;

    public PlayerDisconnectedEvent(Object source, String roomId, Long playerId, boolean isUnexpected) {
        super(source);
        this.roomId = roomId;
        this.playerId = playerId;
        this.isUnexpected = isUnexpected;
    }
}
