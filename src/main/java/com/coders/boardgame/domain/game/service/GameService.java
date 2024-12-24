package com.coders.boardgame.domain.game.service;

import com.coders.boardgame.domain.game.dto.GameRoomDto;
import com.coders.boardgame.domain.game.dto.GameStateDto;
import com.coders.boardgame.domain.game.dto.PlayerDto;
import com.coders.boardgame.domain.game.dto.VoteResultDto;
import com.coders.boardgame.domain.habitsurvey.repository.HabitSurveyResultRepository;
import com.coders.boardgame.domain.user.entity.User;
import com.coders.boardgame.domain.user.repository.UserRepository;
import com.coders.boardgame.exception.GameRoomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final GameRoomService gameRoomService;
    private final GameSseService gameSseService;

    /**
     * 게임 시작 시 게임정보를 SSE로 방에 있는 모든 클라이언트한테 전달
     * @param roomId 방 id
     * @param hostId 호스트 id
     */
    public void startGame(String roomId, Long hostId){
        GameRoomDto room = gameRoomService.getRoom(roomId);

        // 호스트 확인
        if (!room.getHostId().equals(hostId)) {
            throw new GameRoomException("게임을 시작할 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }

        // 참가자 수 확인
        if (room.getCurrentPlayers().get() < room.getTotalPlayers()){
            throw new GameRoomException("모든 참가자가 준비되지 않습니다.", HttpStatus.CONFLICT);
        }

        // 모든 조건이 충족되었으므로 게임 시작 상태 변경
        if (!room.getIsGameStarted().compareAndSet(false,true)){
            throw new GameRoomException("이미 게임이 시작되었습니다.", HttpStatus.CONFLICT);
        }

        // 게임 상태 초기화
        room.setCurrentTurn(1);
        room.setCurrentPuzzlePieces(0);

        // GameStateDto 생성
        GameStateDto gameState = GameStateDto.builder()
                .roomId(roomId)
                .roomName(room.getRoomName())
                .totalPlayers(room.getTotalPlayers())
                .totalPuzzlePieces(room.getTotalPuzzlePieces())
                .currentPuzzlePieces(room.getCurrentPuzzlePieces())
                .currentTurn(room.getCurrentTurn())
                .players(new ArrayList<>(room.getPlayers().values()))
                .build();

        // 게임 시작 알림
        gameSseService.sendRoomEvent(roomId, "game-started", gameState);
        log.info("게임 시작됨: roomId={}, hostId={}", roomId, hostId);
    }

}
