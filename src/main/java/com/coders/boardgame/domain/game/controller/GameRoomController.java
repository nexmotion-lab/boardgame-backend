package com.coders.boardgame.domain.game.controller;

import com.coders.boardgame.domain.game.dto.*;
import com.coders.boardgame.domain.game.service.GameRoomService;
import com.coders.boardgame.domain.user.service.SessionService;
import com.coders.boardgame.exception.GameRoomException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;

@Slf4j
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class GameRoomController {

    private final GameRoomService gameRoomService;
    private final SessionService sessionService;

    /**
     * 방 생성 API
     */
    @PostMapping
    public ResponseEntity<CreateRoomResponseDto> createRoom(
            @RequestBody CreateRoomRequestDto createRoomRequestDto,
            HttpServletRequest request) {
        Long userId = sessionService.getUserIdFromSession(request);

        CreateRoomResponseDto createRoomResponseDto = gameRoomService.createRoom(createRoomRequestDto, userId);
        return ResponseEntity.ok(createRoomResponseDto);
    }

    /**
     * 방 정보 조회 API
     * @param roomId 방ID
     * @return GameRoomDto 객체
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<WaitingRoomDto> getGameRoom(@PathVariable String roomId) {
        GameRoomDto room = gameRoomService.getRoom(roomId);
        WaitingRoomDto waitingRoomDto = gameRoomService.buildWaitingRoomDto(room);
        return ResponseEntity.ok(waitingRoomDto);
    }

    /**
     * 방 참가 API
     * @param roomId 방 id
     * @param joinRoomRequestDto 방 참여 요청 dto
     * @param request request 객체
     * @return ResponseEntity<String>
     */
    @PostMapping("/{roomId}/players")
    public ResponseEntity<WaitingRoomDto> joinRoom(
            @PathVariable String roomId,
            @RequestBody JoinRoomRequestDto joinRoomRequestDto,
            HttpServletRequest request) {

        Long userId = sessionService.getUserIdFromSession(request);

        WaitingRoomDto waitingRoomDto = gameRoomService.joinRoom(roomId, userId, joinRoomRequestDto);
        return ResponseEntity.ok(waitingRoomDto);

    }

    /**
     * 방 나가기 API
     * @param roomId 방 id
     * @param request request 객체
     * @return ResponseEntity<String>
     */
    @DeleteMapping("/{roomId}/players")
    public ResponseEntity<String> leaveRoom(
            @PathVariable String roomId, HttpServletRequest request) {

        Long userId = sessionService.getUserIdFromSession(request);

        gameRoomService.leaveRoom(roomId, userId, true);
        return ResponseEntity.ok().build();

    }

    @PostMapping("/{roomId}/ping")
    public ResponseEntity<?> ping(
            @PathVariable String roomId,
            HttpServletRequest request
    ){
        Long userId = sessionService.getUserIdFromSession(request);
        gameRoomService.updatePingTime(roomId, userId);
        return ResponseEntity.ok().body("pong");
    }

}
