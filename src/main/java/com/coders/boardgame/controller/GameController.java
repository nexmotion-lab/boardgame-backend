package com.coders.boardgame.controller;

import com.coders.boardgame.dto.GameRoomDto;
import com.coders.boardgame.dto.PlayerDto;
import com.coders.boardgame.dto.VoteResultDto;
import com.coders.boardgame.exception.GameRoomException;
import com.coders.boardgame.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games/room")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @PostMapping
    public ResponseEntity<GameRoomDto> createGameRoom(@RequestParam Long userId, @RequestParam int headCount) {
        GameRoomDto room = gameService.createGameRoom(userId, headCount);
        return ResponseEntity.ok(room);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<GameRoomDto> getGameRoom(@PathVariable String roomId) {
        GameRoomDto room = gameService.getGameRoom(roomId);
        return ResponseEntity.ok(room);
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<String> joinGameRoom(@PathVariable String roomId, @RequestBody PlayerDto player) {
        try {
            gameService.addPlayer(roomId, player);
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
