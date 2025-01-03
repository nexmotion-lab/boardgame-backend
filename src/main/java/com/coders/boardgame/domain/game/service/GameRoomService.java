package com.coders.boardgame.domain.game.service;

import com.coders.boardgame.domain.game.dto.*;
import com.coders.boardgame.exception.GameRoomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 게임 방 관련 service
 */
@Slf4j
@Service
@RequiredArgsConstructor
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
                .surveyScore(requestDto.getSurveyScore())
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
                .isGameStarted(new AtomicBoolean(false))
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
     * @param roomId 방 id
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
     * @param roomId 방 id
     * @return SseEmitter 객체
     */
    public SseEmitter connectToRoom(String roomId, Long playerId) {
        GameRoomDto room = getRoom(roomId);

        SseEmitter emitter = gameSseService.connectToRoom(roomId, playerId);

        WaitingRoomDto waitingRoomDto = WaitingRoomDto.builder()
                .roomId(room.getRoomId())
                .roomName(room.getRoomName())
                .currentPlayers(room.getCurrentPlayers().get())
                .totalPlayers(room.getTotalPlayers())
                .players(new ArrayList<>(room.getPlayers().values()))
                .build();

        // 클라이언트에 초기 연결 상태 전송
        try{
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(waitingRoomDto)
            );
        } catch (IOException e){
            log.error("방 상태 전송 실패: roomId={}, playerId={}, error={}", roomId, playerId, e.getMessage());
            emitter.completeWithError(e);
        }

        // 방에 연결을 완료했다고 자신을 제외한 모든인원들한테 방상태를 보냄
        gameSseService.sendRoomEventToOthers(roomId, "player-joined", room.getPlayers().get(playerId), playerId);

        return emitter;
    }

    /**
     * 방 참가
     * @param roomId 방 ID
     * @param userId 유저 ID
     * @param joinRoomRequestDto 방 참여 요구 DTO
     * @return playerDto
     */
    public WaitingRoomDto joinRoom(String roomId, Long userId, JoinRoomRequestDto joinRoomRequestDto){
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


        // WaitingRoomDto 생성

        return WaitingRoomDto.builder()
                .roomId(roomId)
                .roomName(room.getRoomName())
                .currentPlayers(currentPlayers)
                .totalPlayers(room.getTotalPlayers())
                .players(new ArrayList<>(room.getPlayers().values()))
                .build();
    }

    /**
     * 방 나가기
     * 만약 방장이 나가게 되면 무작위로 방장이 배정되고 전부 방에 나갔을 때는 방을 폭파시킴
     * @param roomId 방 id
     * @param playerId 사용자 id
     */
    public void leaveRoom(String roomId, Long playerId) {

        // 방 존재 확인
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
            assignNewHost(room);
        }

        // 방 상태 최신화 전송
        if (currentPlayers > 0) {
            gameSseService.sendRoomEventToOthers(room.getRoomId(), "player-left", playerId, playerId);
        } else {
            deleteRoom(roomId); // 방에 아무도 없으면 삭제
        }
        // SSE 연결 해제
        gameSseService.disconnectPlayer(roomId, "player-left", playerId);
    }


    /**
     * 호스트 재선정
     * @param room
     */
    private void assignNewHost(GameRoomDto room) {
        Long newHostId = room.getPlayers().keySet().iterator().next(); // 남은 플레이어 중 하나를 방장으로 설정
        room.setHostId(newHostId);

        // 새방장의 이름 가져오기
        String newHostName = room.getPlayers().get(newHostId).getPlayerInfo().getName();

        room.getPlayers().forEach((playerId, player) ->{
            String message;
            String eventName;

            if(playerId.equals(newHostId)){
                // 새 방장에게 알림
                message = "당신이 새로운 방장이 되었습니다";
                eventName = "host-assigned";
            } else {
                // 다른 사용자들에게 새 방장 정보 알림
                message = "새로운 방장은 " + newHostName + "입니다.";
                eventName = "host-changed";
            }

            // 특정 사용자에게 이벤트 전송
            gameSseService.sendToSpecificPlayer(room.getRoomId(), playerId, eventName, message);
        });

        log.info("새로운 방장이 지정되었습니다: roomId={}, newHostId={}", room.getRoomId(), newHostId);
    }

    /**
     * 방 삭제 함수
     * @param roomId
     */
    private void deleteRoom(String roomId) {
        gameRooms.remove(roomId);
        gameSseService.removeEmitters(roomId);
        log.info("방이 삭제되었습니다: roomId={}", roomId);
    }
}
