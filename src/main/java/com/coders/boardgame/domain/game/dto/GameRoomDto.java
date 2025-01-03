package com.coders.boardgame.domain.game.dto;

import lombok.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 서버 내부에서 상태 관리 및 로직 처리용
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameRoomDto {
    private String roomId;  // 방 Id
    private String roomName; // 방 이름
    private int totalPlayers; // 최대 플레이어 수
    private AtomicInteger currentPlayers; // 현재 플레이어 수
    private Long hostId;  // 방장  Id
    private Map<Long, PlayerDto> players = new ConcurrentHashMap<>();
    private int currentTurn; // 현재 턴을 수행중인 플레이어의 순번
    private int totalPuzzlePieces; // 총 퍼즐 조각 개수
    private int currentPuzzlePieces; // 현재 획득한 퍼즐 조각 개수
    private int currentRound;
    private int puzzleImgId; // 퍼즐 이미지 id
    private AtomicBoolean isGameStarted = new AtomicBoolean(false); // 게임 시작 여부
    private boolean isPictureCardAssigned = false;
    private boolean isTextCardAssigned = false;
    private boolean hasReVoted = false;
}
