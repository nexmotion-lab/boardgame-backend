package com.coders.boardgame.domain.game.controller;

import com.coders.boardgame.domain.game.dto.*;
import com.coders.boardgame.domain.game.service.GameRoomService;
import com.coders.boardgame.domain.user.dto.UserDto;
import com.coders.boardgame.domain.user.service.SessionService;
import com.coders.boardgame.exception.GameRoomException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/games/rooms")
@RequiredArgsConstructor
public class GameRoomController {

    private final GameRoomService gameRoomService;
    private final SessionService sessionService;

    /**
     * 방 생성
     */
    @PostMapping("/create")
    public ResponseEntity<CreateRoomResponseDto> createRoom(
            @RequestBody CreateRoomRequestDto createRoomRequestDto,
            HttpServletRequest request) {
        Long userId = sessionService.getUserIdFromSession(request);

        CreateRoomResponseDto createRoomResponseDto = gameRoomService.createRoom(createRoomRequestDto, userId);
        return ResponseEntity.ok(createRoomResponseDto);
    }

    /**
     * 방 정보 조회
     * @param roomId 방ID
     * @return GameRoomDto 객체
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<GameRoomDto> getGameRoom(@PathVariable String roomId) {
        GameRoomDto room = gameRoomService.getRoom(roomId);
        log.info(room.getPlayers().toString());
        return ResponseEntity.ok(room);
    }

    /**
     * 방 참가
     *
     * @param roomId 방 id
     * @param joinRoomRequestDto 방 참여 요청 dto
     * @param request request 객체
     * @return ResponseEntity<String>
     */
    @PostMapping("/{roomId}/join")
    public ResponseEntity<String> joinRoom(
            @PathVariable String roomId,
            @RequestBody JoinRoomRequestDto joinRoomRequestDto,
            HttpServletRequest request) {

        Long userId = sessionService.getUserIdFromSession(request);

        try {
            PlayerDto player = gameRoomService.joinRoom(roomId, userId, joinRoomRequestDto);
            return ResponseEntity.ok("플레이어 " + player.getPlayerInfo().getName() + "가 게임방(" + roomId + ")에 참여했습니다.");
        } catch (GameRoomException e) {
            return ResponseEntity.status(e.getStatus()).body(e.getMessage());
        }
    }

    /**
     * 방 나가기
     *
     * @param roomId 방 id
     * @param request request 객체
     * @return ResponseEntity<String>
     */
    @PostMapping("/{roomId}/leave")
    public ResponseEntity<String> leaveRoom(
            @PathVariable String roomId, HttpServletRequest request) {

        Long userId = sessionService.getUserIdFromSession(request);

        try {
            gameRoomService.leaveRoom(roomId, userId);
            return ResponseEntity.ok().build();
        } catch (GameRoomException e) {
            return ResponseEntity.status(e.getStatus()).body(e.getMessage());
        }
    }

    /**
     * 방과 SSE 연결
     * @param roomId 방 id
     * @param request request 객체
     * @return
     */
    @GetMapping(value = "/connect/{roomId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectToRoom(@PathVariable String roomId, HttpServletRequest request){
        Long userId = sessionService.getUserIdFromSession(request);
        return gameRoomService.connectToRoom(roomId, userId);
    }
}
