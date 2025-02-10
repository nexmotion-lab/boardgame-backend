package com.coders.boardgame.domain.game.controller;

import com.coders.boardgame.domain.game.service.GameRoomService;
import com.coders.boardgame.domain.user.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class GameSseController {

    private final GameRoomService gameRoomService;
    private final SessionService sessionService;

    /**
     * 방과 SSE 연결
     * @param roomId 방 id
     * @param request request 객체
     * @return emitter 객체
     */
    @GetMapping(value = "/rooms/connect/{roomId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectToRoom(@PathVariable String roomId, HttpServletRequest request){
        log.info("커넥 컨트롤러 도달");
        return gameRoomService.connectToRoom(roomId, sessionService.getUserIdFromSession(request));
    }
}
