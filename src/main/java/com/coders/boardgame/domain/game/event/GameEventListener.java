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

        String roomId = event.getRoomId();
        Long playerId = event.getPlayerId();

        GameRoomDto room;
        // 이미 방이 삭제된 경우에 대해서
        try {
            room = gameRoomService.getRoom(roomId);
        } catch (GameRoomException e){
            log.warn("이미 방이 삭제되었거나 존재하지 않습니다. {}", e.getMessage());
            return;
        }

        gameRoomService.leaveRoom(roomId, playerId, false);

    }

    @EventListener
    public void handleGameEnded(GameEndedEvent event){
        String roomId = event.getRoomId();
        log.info("GameEndedEvent 수신: roomId={}", roomId);

        // 게임 상태 초기화 및 리셋
        gameService.resetGame(roomId);

        GameRoomDto room;
        // 이미 방이 삭제된 경우에 대해서
        try {
            room = gameRoomService.getRoom(roomId);
        } catch (GameRoomException e){
            log.warn("이미 방이 삭제되었거나 존재하지 않습니다. {}", e.getMessage());
            return;
        }

    }
}
