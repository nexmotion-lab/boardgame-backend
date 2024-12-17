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
@RequestMapping("/api/games/room")
@RequiredArgsConstructor
@Slf4j
public class GameController {

    private final GameService gameService;
    private final SessionService sessionService;

    @PostMapping
    public ResponseEntity<String> createGameRoom(@RequestBody GameRoomDto room, HttpServletRequest request) {
        Long userId = sessionService.getUserIdFromSession(request);

        String roomId = gameService.createGameRoom(userId, room);
        log.info("방 생성 완료: " + roomId);
        return ResponseEntity.ok(roomId);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<GameRoomDto> getGameRoom(@PathVariable String roomId) {
        GameRoomDto room = gameService.getGameRoom(roomId);
        log.info(room.getPlayers().toString());
        return ResponseEntity.ok(room);
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<String> joinGameRoom(@PathVariable String roomId, HttpServletRequest request) {
        Long userId = sessionService.getUserIdFromSession(request);

        try {
            PlayerDto player = gameService.addPlayer(roomId, userId);
            return ResponseEntity.ok("플레이어 " + player.getName() + "가 게임방(" + roomId + ")에 참여했습니다.");
        } catch (GameRoomException e) {
            return ResponseEntity.status(e.getStatus()).body(e.getMessage());
        }
    }

    @PostMapping("/{roomId}/submit")
    public ResponseEntity<Void> submitUsedTime(@PathVariable String roomId, @RequestBody PlayerDto player) {
        gameService.submitUsedTime(roomId, player);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{roomId}/start")
    public ResponseEntity<PlayerDto> startGame(@PathVariable String roomId) {
        PlayerDto currentPlayer = gameService.startGame(roomId);
        return ResponseEntity.ok(currentPlayer);
    }

    @PostMapping("/{roomId}/vote")
    public ResponseEntity<VoteResultDto> vote(@PathVariable String roomId, @RequestParam Long userId, @RequestParam boolean isAgreed) {
        VoteResultDto result = gameService.vote(roomId, userId, isAgreed);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{roomId}/ranking")
    public ResponseEntity<List<PlayerDto>> calculateRankings(@PathVariable String roomId) {
        List<PlayerDto> result = gameService.calculateRankings(roomId);

        if (result.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        return ResponseEntity.ok(result);
    }
}
