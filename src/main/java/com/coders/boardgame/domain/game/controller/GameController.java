package com.coders.boardgame.domain.game.controller;

import com.coders.boardgame.domain.game.dto.GameRoomDto;
import com.coders.boardgame.domain.game.dto.PlayerDto;
import com.coders.boardgame.domain.game.dto.VoteResultDto;
import com.coders.boardgame.domain.user.service.SessionService;
import com.coders.boardgame.exception.GameRoomException;
import com.coders.boardgame.domain.game.service.GameService;
import com.coders.boardgame.exception.auth.CustomSessionAuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
@Slf4j
public class GameController {

    private final GameService gameService;
    private final SessionService sessionService;

    /**
     * 게임 시작 API
     * @param roomId
     * @param request
     * @return
     */
    @PostMapping("/{roomId}/state")
    public ResponseEntity<PlayerDto> startGame(@PathVariable String roomId, HttpServletRequest request) {

        Long userId = sessionService.getUserIdFromSession(request);
        gameService.startGame(roomId, userId);
        return ResponseEntity.ok().build();
    }

}
