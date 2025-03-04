package com.coders.boardgame.domain.game.controller;

import com.coders.boardgame.domain.game.dto.*;
import com.coders.boardgame.domain.user.service.SessionService;
import com.coders.boardgame.domain.game.service.GameService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     * 게임 상태 조희 API
     *
     * @param roomId  방 정보
     * @param request 클라이언트 요청 객체
     * @return
     */
    @GetMapping("/{roomId}/state")
    public ResponseEntity<GameStateDto> getGameState(@PathVariable String roomId, HttpServletRequest request) {

        Long userId = sessionService.getUserIdFromSession(request);
        GameStateDto gameState = gameService.getGameState(roomId, userId);
        return ResponseEntity.ok(gameState);

    }

    /**
     * 게임 중 타이머 시간 동기화를 위한 API
     *
     * @param roomId
     * @return
     */
    @GetMapping("/{roomId}/time-sync")
    public ResponseEntity<TimeSyncDto> getTimeSync(@PathVariable String roomId) {
        long serverTime = System.currentTimeMillis();
        TimeSyncDto timeSyncDto = new TimeSyncDto(serverTime);
        return ResponseEntity.ok(timeSyncDto);
    }

    /**
     * 게임 시작 API
     *
     * @param roomId  방 ID
     * @param request 클라이언트 요청 객체
     * @return
     */
    @PostMapping("/{roomId}/round/1/state")
    public ResponseEntity<String> startGame(@PathVariable String roomId,
                                            HttpServletRequest request) {

        Long userId = sessionService.getUserIdFromSession(request);
        gameService.startGame(roomId, userId);
        return ResponseEntity.ok("게임 시작을 완료했습니다.");
    }

    /**
     * 사용 시간 입력 API
     *
     * @param roomId    룸 ID
     * @param usageTime 스마트폰 사용시간
     * @param request   요청
     * @return
     */
    @PostMapping("/{roomId}/round/1/usage-time")
    public ResponseEntity<String> setUsageTime(@PathVariable String roomId,
                                               @RequestParam int usageTime,
                                               HttpServletRequest request) {
        Long userId = sessionService.getUserIdFromSession(request);

        gameService.setUsageTime(roomId, userId, usageTime);
        return ResponseEntity.ok("사용 시간 입력 완료");
    }

    /**
     * 사용자 순서 배정 이후
     * 게임 라운드 2를 시작하기 위한 API
     *
     * @param roomId
     * @param request
     * @return
     */
    @PostMapping("/{roomId}/round/2/state")
    public ResponseEntity<String> startRoundTwo(
            @PathVariable String roomId,
            HttpServletRequest request) {
        Long userId = sessionService.getUserIdFromSession(request);

        gameService.startRound(roomId, 2, userId);
        return ResponseEntity.ok("2라운드 시작");
    }

    /**
     * 말하기 세션이 시작하기 위해
     * 랜덤으로 카드를 부여를 처리하기 위한 API
     *
     * @param roomId   룸 id
     * @param cardType 카드타입
     * @param maxId    보유한 카드의 최대 id
     * @return
     */
    @PostMapping("/{roomId}/card/{cardType}")
    public ResponseEntity<String> assignCard(
            @PathVariable String roomId,
            @PathVariable String cardType,
            @RequestParam int maxId) {
        gameService.assignCard(roomId, maxId, cardType);
        return ResponseEntity.ok(cardType + "카드가 부여되었습니다.");
    }

    /**
     * 말하기 세션중 시간 연장을 처리하는 API
     *
     * @param roomId         방Id
     * @param additionalTime 추가할 시간
     * @return
     */
    @PostMapping("/{roomId}/timer/extensions")
    public ResponseEntity<String> extendTime(
            @PathVariable String roomId,
            @RequestParam int additionalTime) {
        gameService.extendTime(roomId, additionalTime);
        return ResponseEntity.ok("시간이 연장되었습니다.");
    }

    /**
     * 종료된 말하기 세션을 처리하는 API
     *
     * @param roomId
     * @return
     */
    @PostMapping("/{roomId}/speaking/end")
    public ResponseEntity<String> endSpeaking(@PathVariable String roomId) {
        gameService.endSpeaking(roomId);
        return ResponseEntity.ok("말하기가 종료되었습니다.");
    }

    /**
     * 투표 API
     *
     * @param roomId  방 ID
     * @param voteRequestDto    투표 내용
     * @param request 클라이언트 요청 객체
     * @return 투표 성공 메세지
     */
    @PostMapping("/{roomId}/votes")
    public ResponseEntity<String> castVote(
            @PathVariable String roomId,
            @RequestBody VoteRequestDto voteRequestDto,
            HttpServletRequest request) {
        Long userId = sessionService.getUserIdFromSession(request);

        String vote = voteRequestDto.getVote();
        gameService.castVote(roomId, userId, vote);

        return ResponseEntity.ok("투표 완료했습니다.");
    }

    /**
     * 게임 다시하기 요청
     * @param roomId
     * @param request
     * @return
     */
    @PostMapping("/{roomId}/retry")
    public ResponseEntity<String> retryGame(@PathVariable String roomId, HttpServletRequest request) {
        Long userId = sessionService.getUserIdFromSession(request);
        gameService.retryGame(roomId, userId);
        return ResponseEntity.ok("다시하기 완료");
    }
}
