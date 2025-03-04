package com.coders.boardgame.domain.game.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PlayerReadyCanceledEvent extends ApplicationEvent {
    private final String roomId;
    private final Long playerId;

    public PlayerReadyCanceledEvent(Object source, String roomId, Long playerId) {
        super(source);
        this.roomId = roomId;
        this.playerId = playerId;
    }
}
