package com.coders.boardgame.domain.game.service;

import com.coders.boardgame.domain.game.dto.*;
import com.coders.boardgame.exception.GameRoomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 게임 방 관련 service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameRoomService {

    // 방 관리 : 방 ID -> 방정보
    private final Map<String, GameRoomDto> gameRooms = new ConcurrentHashMap<>();

    // 게임 SSE 서비스
    private final GameSseService gameSseService;


    /**
     * UUID 기반 8자리 Room ID 생성
     * @return roomId
     */
    public String generateRoomId() {
        int attempts = 0;
        int maxAttempts = 100;

        String roomId;
        do {
            if (attempts++ >= maxAttempts) {
                throw new IllegalStateException("Room ID 생성에 실패했습니다.");
            }
            roomId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        } while (gameRooms.containsKey(roomId));

        return roomId;
    }

    /**
     * 게임 방 생성 함수
     * @param requestDto
     * @return CreateRoomResponseDto 객체 반환
     */
    public CreateRoomResponseDto createRoom(CreateRoomRequestDto requestDto, Long userId) {
        String roomId = generateRoomId();

        // 방장 생성
        PlayerDto host = PlayerDto.builder()
                .playerId(userId)
                .playerInfo(requestDto.getHostInfo())
                .sequenceNumber(0)
                .collectedPuzzlePieces(0)
                .isSpeaking(false)
                .build();

        // 방 정보 생성
        GameRoomDto gameRoom = GameRoomDto.builder()
                .roomId(roomId)
                .roomName(requestDto.getRoomName())
                .totalPlayers(requestDto.getTotalPlayers())
                .currentPlayers(new AtomicInteger(1))
                .hostId(host.getPlayerId())
                .players(new ConcurrentHashMap<>(Map.of(host.getPlayerId(), host)))
                .currentTurn(0)
                .totalPuzzlePieces(requestDto.getTotalPlayers() == 3 ? 10 : 13)
                .currentPuzzlePieces(0)
                .isGameStarted(false)
                .build();

        // 생성된 방 저장
        gameRooms.put(roomId, gameRoom);

        return CreateRoomResponseDto.builder()
                .roomId(roomId)
                .roomName(requestDto.getRoomName())
                .totalPlayers(requestDto.getTotalPlayers())
                .host(host)
                .build();
    }

    /**
     * 방 조회 함수
     * @param roomId
     * @return room 정보 반환
     */
    public GameRoomDto getRoom(String roomId){
        GameRoomDto room = gameRooms.get(roomId);
        if(room == null){
            throw new GameRoomException("방이 존재하지 않습니다: " + roomId, HttpStatus.NOT_FOUND);
        }
        return room;
    }

    /**
     * 생성된 방 sse 연결
     * @param roomId
     * @return SseEmitter 객체
     */
    public SseEmitter connectToRoom(String roomId, Long playerId) {
        if (!gameRooms.containsKey(roomId)) {
            throw new GameRoomException("방이 존재하지 않습니다: " + roomId, HttpStatus.NOT_FOUND);
        }
        return gameSseService.connectToRoom(roomId, playerId);
    }

    /**
     * 방 참가
     * @param roomId 방 ID
     * @param userId 유저 ID
     * @param joinRoomRequestDto 방 참여 요구 DTO
     * @return playerDto
     */
    public PlayerDto joinRoom(String roomId, Long userId, JoinRoomRequestDto joinRoomRequestDto){
        GameRoomDto room = getRoom(roomId);

        // 방이 있는지 확인
        if (room == null){
            throw new GameRoomException("방이 존재하지 않습니다: " + roomId, HttpStatus.NOT_FOUND);
        }

        // 플레이어가 이미 존재하는지 확인
        if (room.getPlayers().containsKey(userId)) {
            throw new GameRoomException(userId + " 이미 방에 있습니다.", HttpStatus.FORBIDDEN);
        }

        // 현재 플레이어 수 확인
        if (room.getCurrentPlayers().get() >= room.getTotalPlayers()) {
            throw new GameRoomException("방이 가득찼습니다: " + roomId, HttpStatus.FORBIDDEN);
        }

        // 플레이어 생성
        PlayerDto player = PlayerDto.builder()
                .playerId(userId)
                .playerInfo(joinRoomRequestDto.getUserInfo())
                .sequenceNumber(0)
                .collectedPuzzlePieces(0)
                .usageTime(0)
                .surveyScore(joinRoomRequestDto.getSurveyScore())
                .isSpeaking(false)
                .build();

        // 플레이어 리스트 업데이트에 동기화 적용
        room.getPlayers().put(userId, player);
        int currentPlayers = room.getCurrentPlayers().incrementAndGet();

        // 방 전체 참가자 정보 전송
        List<PlayerDto> allPlayers = new ArrayList<>(room.getPlayers().values());
        gameSseService.sendRoomEvent(roomId, "all-players", allPlayers);

        // 참가자가 최대치에 도달하면 호스트에게 게임 시작 가능 이벤트 전송
        if (currentPlayers == room.getTotalPlayers()) {
            Long hostId = room.getHostId();
            gameSseService.sendToSpecificPlayer(roomId, hostId, "game-ready", "게임을 시작할 수 있습니다.");
        }

        return player;
    }

    /**
     * 방 나가기
     * 만약 방장이 나가게 되면 무작위로 방장이 배정되고 전부 방에 나갔을 때는 방을 폭파시킴
     * @param roomId 방 id
     * @param playerId 사용자 id
     */
    public void leaveRoom(String roomId, Long playerId) {
        GameRoomDto room = gameRooms.get(roomId);
        if(room == null) {
            throw new GameRoomException("방이 존재하지 않습니다: " + roomId, HttpStatus.NOT_FOUND);
        }

        // 플레이어 제거
        PlayerDto removedPlayer = room.getPlayers().remove(playerId);
        if (removedPlayer == null){
            throw new GameRoomException( "플레이어가 방에 존재하지 않습니다.", HttpStatus.NOT_FOUND);
        }

        // 현재 플레이어 수 감소
        int currentPlayers = room.getCurrentPlayers().decrementAndGet();// 현재 플레이어수 감소

        // 방장이 나간 경우 새로운 방장 무작위로 선정
        if (room.getHostId().equals(playerId) && currentPlayers > 0) {
            Long newHostId = room.getPlayers().keySet().iterator().next(); // 남은 플레이어 중 하나를 방장으로 설정
            room.setHostId(newHostId);
            gameSseService.sendRoomEvent(roomId, "host-changed", newHostId);
        }

        // SSE 연결 해제
        gameSseService.disconnectPlayer(roomId, "player-left", playerId);

        // SSE로 나가기 이벤트 전송 및 emitter 끊기
        gameSseService.sendRoomEvent(roomId, "player-left", playerId);


        // 방에 아무도 없으면 방 삭제
        if (currentPlayers == 0) {
            gameRooms.remove((roomId));
            gameSseService.removeEmitters(roomId);
        }
    }
}
