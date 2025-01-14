package com.coders.boardgame.domain.game.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 게임이 종료되었을 때 발생하는 이벤트
 */
@Getter
public class GameEndedEvent extends ApplicationEvent {
    private final String roomId;

    public GameEndedEvent(Object source, String roomId) {
        super(source);
        this.roomId = roomId;
    }

}
