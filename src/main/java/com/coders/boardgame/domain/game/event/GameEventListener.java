package com.coders.boardgame.domain.game.event;

import com.coders.boardgame.domain.game.dto.GameRoomDto;
import com.coders.boardgame.domain.game.service.GameRoomService;
import com.coders.boardgame.domain.game.service.GameService;
import com.coders.boardgame.exception.GameRoomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameEventListener {

    private final GameService gameService;
    private final GameRoomService gameRoomService;

    /**
     * 플레이어가 비정상적으로 연결이 끊긴 경우 이벤트 처리
     * @param event
     */
    @EventListener
    public void handlePlayerDisconnected(PlayerDisconnectedEvent event){
        // 의도적으로 끊김이면 여기서 처리할 필요없이 return
        if (!event.isUnexpected()) {
            return;
        }
        gameRoomService.leaveRoom(event.getRoomId(), event.getPlayerId(), false);
    }

    /**
     * 게임이 끝났다는 이벤트를 발생 시, 게임 상태 초기화
     * @param event
     */

    @EventListener
    public void handleGameEnded(GameEndedEvent event){
        String roomId = event.getRoomId();
        // 게임 상태 초기화 및 리셋
        gameService.resetGame(roomId);

    }

    @EventListener
    public void handlePlayerReadyCanceled(PlayerReadyCanceledEvent event) {
        gameRoomService.cancelPlayerReadyStatus(event.getRoomId(), event.getPlayerId());
    }
}
