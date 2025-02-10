package com.coders.boardgame.domain.game.service;

import com.coders.boardgame.domain.game.dto.*;
import com.coders.boardgame.domain.game.enums.RoomStatus;
import com.coders.boardgame.domain.game.event.GameEndedEvent;
import com.coders.boardgame.exception.GameRoomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

    private final ApplicationEventPublisher applicationEventPublisher;

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
                .usageTime(0)
                .sequenceNumber(0)
                .collectedPuzzlePieces(0)
                .lastPingTime(System.currentTimeMillis())
                .isSpeaking(false)
                .isReady(false)
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
                .totalPuzzlePieces(requestDto.getTotalPlayers() == 3 ? 5 : 13)
                .currentPuzzlePieces(0)
                .roomStatus(RoomStatus.WAITING)
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

    public List<GameRoomDto> getAllRooms() {
        return new ArrayList<>(gameRooms.values());
    }

    /**
     * 생성된 방 sse 연결
     * @param roomId 방 id
     * @return SseEmitter 객체
     */
    public SseEmitter connectToRoom(String roomId, Long playerId) {
        GameRoomDto room = getRoom(roomId);

        boolean isInRoom = room.getPlayers().containsKey(playerId);

        log.info("커넥 시작");

        SseEmitter emitter;
        boolean isReconnecting;
        if (!isInRoom) {
            // 방에 없는 사용자
            emitter = new SseEmitter(3000L);
            isReconnecting = false;
            try {
                emitter.send(SseEmitter.event()
                        .name("not-in-room")
                        .data("방에 없습니다. 다시 입장 요청하세요")
                );
            } catch (IOException e) {
                log.error("not-in-room 이벤트 전송 실패: roomId={}, playerId={}, error={}",
                        roomId, playerId, e.getMessage());
                emitter.completeWithError(e);
            }
            emitter.complete();
            return emitter;
        } else {
            // 게임 SSE 서비스에서 연결 시도 및 재연결 여부 확인
            ConnectionResult connectionResult = gameSseService.connectToRoom(roomId, playerId);
            emitter = connectionResult.emitter();
            isReconnecting = connectionResult.isReconnecting();
        }

        // 방 상태에 따른 처리
        switch (room.getRoomStatus()){
            case ENDED:
                // 게임이 종료 된 경우 "game-ended" 이벤트 전송
                try {
                    emitter.send(SseEmitter.event()
                            .name("game-ended")
                            .data("게임이 이미 종료됨")
                    );
                } catch (IOException e) {
                    log.error("게임 종료 알림 전송 실패: roomId={}, playerId={}, error={}", roomId, playerId, e.getMessage());
                    emitter.completeWithError(e);
                }
                return emitter;

            case WAITING:
                room.getPlayers().computeIfPresent(playerId, (id, p) -> {
                    p.setReady(true);
                    return p;
                });

                // 클라이언트에 초기 연결 상태 전송
                try{
                    emitter.send(SseEmitter.event()
                            .name("connected")
                            .data("연결완료")
                    );
                } catch (IOException e){
                    log.error("방 상태 전송 실패: roomId={}, playerId={}, error={}", roomId, playerId, e.getMessage());
                    emitter.completeWithError(e);
                }

                // 방에 연결을 완료했다고 자신을 제외한 모든인원들한테 방상태를 보냄
                if(isReconnecting){
                    gameSseService.sendRoomEventToOthers(roomId, "player-reconnected", room.getPlayers().get(playerId), playerId);
                } else {
                    gameSseService.sendRoomEventToOthers(roomId, "player-joined", room.getPlayers().get(playerId), playerId);
                }
                return emitter;

            case IN_GAME:
                // 게임 중인 상태일 때 연결 처리
                GameStateDto gameState = GameStateDto.builder()
                        .roomId(roomId)
                        .roomName(room.getRoomName())
                        .hostId(room.getHostId())
                        .totalPlayers(room.getTotalPlayers())
                        .totalPuzzlePieces(room.getTotalPuzzlePieces())
                        .currentPuzzlePieces(room.getCurrentPuzzlePieces())
                        .currentTurn(room.getCurrentTurn())
                        .currentPhase(room.getCurrentPhase())
                        .currentRound(room.getCurrentRound())
                        .players(new ArrayList<>(room.getPlayers().values()))
                        .build();

                try {
                    emitter.send(SseEmitter.event()
                            .name("game-connected")
                            .data(gameState)
                    );
                } catch (IOException e) {
                    log.error("게임 상태 전송 실패: roomId={}, playerId={}, error={}", roomId, playerId, e.getMessage());
                    emitter.completeWithError(e);
                }

                if (isReconnecting) {
                    gameSseService.sendRoomEventToOthers(roomId, "player-reconnected", room.getPlayers().get(playerId), playerId);
                }
                return emitter;

            default:
                // 예상치 못한 상태 에러
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("알 수 없는 방 상태입니다")
                    );
                } catch (IOException e) {
                    log.error("알 수 없는 방 상태 알림 전송 실패: roomId={}, playerId={}, error={}", roomId, playerId, e.getMessage());
                    emitter.completeWithError(e);
                }
                gameSseService.disconnectPlayer(roomId, "unknown-state", playerId, true);
                throw new GameRoomException("알 수 없는 방 상태 알림 전송 실패", HttpStatus.BAD_REQUEST);
        }
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

        if (room.getRoomStatus().equals(RoomStatus.IN_GAME) || room.getRoomStatus().equals(RoomStatus.ENDED)){
            throw new GameRoomException("게임이 시작되거나 종료중입니다. 잠시후 다시 시도해주세요", HttpStatus.FORBIDDEN);
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
                .isReady(false)
                .lastPingTime(System.currentTimeMillis())
                .build();

        // 플레이어 리스트 업데이트에 동기화 적용
        room.getPlayers().put(userId, player);
        int currentPlayers = room.getCurrentPlayers().incrementAndGet();


        return WaitingRoomDto.builder()
                .roomId(roomId)
                .roomName(room.getRoomName())
                .currentPlayers(currentPlayers)
                .totalPlayers(room.getTotalPlayers())
                .hostId(room.getHostId())
                .players(new ArrayList<>(room.getPlayers().values()))
                .roomStatus(room.getRoomStatus())
                .build();
    }

    /**
     * 방 나가기
     * 만약 방장이 나가게 되면 무작위로 방장이 배정되고 전부 방에 나갔을 때는 방을 폭파시킴
     * @param roomId 방 id
     * @param playerId 사용자 id
     */
    public void leaveRoom(String roomId, Long playerId, boolean isIntentional) {

        // 방 존재 확인
        GameRoomDto room = gameRooms.get(roomId);
        if (room == null) {
            throw new GameRoomException("방이 존재하지 않습니다: " + roomId, HttpStatus.NOT_FOUND);
        }

        // 플레이어 제거
        PlayerDto removedPlayer = room.getPlayers().remove(playerId);
        if (removedPlayer == null){
            throw new GameRoomException( "플레이어가 방에 존재하지 않습니다.", HttpStatus.NOT_FOUND);
        }

        // 현재 플레이어 수 감소
        int currentPlayers = room.getCurrentPlayers().decrementAndGet();// 현재 플레이어수 감소

        if (isIntentional){
            gameSseService.disconnectPlayer(roomId, "player-left", playerId, false);
        }

        if (room.getRoomStatus() == RoomStatus.IN_GAME) {
            InterruptGameAndMoveToWaitingRoom(roomId, removedPlayer);

        } else if (room.getRoomStatus() == RoomStatus.WAITING) {
            // 방장이 나간 경우 새로운 방장 무작위로 선정
            if (room.getHostId().equals(playerId) && currentPlayers > 0) {
                assignNewHost(room, true);
            }
            // 방 상태 최신화 전송
            if (currentPlayers > 0) {
                gameSseService.sendRoomEvent(room.getRoomId(), "player-left", playerId);
            } else {
                deleteRoom(roomId); // 방에 아무도 없으면 삭제
            }
        } else if (room.getRoomStatus() == RoomStatus.ENDED) {
            if (currentPlayers <=0){
                deleteRoom(roomId);
            }
        }

        // SSE 연결 해제
    }

    public void reassignHostIfNeed(String roomId){
        GameRoomDto room = gameRooms.get(roomId);
        if (room == null) return;

        // 호스트 id가 없는지 확인
        if (!room.getPlayers().containsKey(room.getHostId()) && !room.getPlayers().isEmpty()) {
            assignNewHost(room, false);
        }

    }

    /**
     * * 게임 종료 및 모든 플레이어를 대기방으로 이동
     * @param roomId
     */
    public void InterruptGameAndMoveToWaitingRoom(String roomId, PlayerDto player){
        GameRoomDto room = gameRooms.get(roomId);

        log.info("IN_GAME 상태에서 플레이어 {}가 나갔으므로 게임 종료 진행", player.getPlayerId());
        String reason = player.getPlayerInfo().getName() + "가 나갔습니다.";

        // 방상태 ended로 변환
        room.setRoomStatus(RoomStatus.WAITING);

        Map<String, String> eventData = Map.of(
                "reason", reason,
                "data", "게임이 리셋되어 대기방으로 이동 필요"
        );

        // 모든 플레이어에게 게임 종료 이벤트 전송
        gameSseService.sendRoomEvent(roomId, "game-reset", eventData);

        // gameEvent 발행
        applicationEventPublisher.publishEvent(new GameEndedEvent(this, roomId));


        log.info("게임이 종료되었습니다: roomId={}", roomId);

    }

    public void updatePingTime(String roomId, Long playerId) {
        GameRoomDto room = gameRooms.get(roomId);
        PlayerDto player = room.getPlayers().get(playerId);

        if (player == null) {
            throw new GameRoomException("플레이어가 방에 없음", HttpStatus.NOT_FOUND);
        }

        player.setLastPingTime(System.currentTimeMillis());
    }

    /**
     * 호스트 재선정
     * @param room
     */
    private void assignNewHost(GameRoomDto room, boolean isEventRequired) {
        Long newHostId = room.getPlayers().keySet().iterator().next(); // 남은 플레이어 중 하나를 방장으로 설정
        room.setHostId(newHostId);

        if(isEventRequired) {
            // 새방장의 이름 가져오기
            String newHostName = room.getPlayers().get(newHostId).getPlayerInfo().getName();

            room.getPlayers().forEach((playerId, player) -> {
                Map<String, Object> eventData = new HashMap<>();
                String eventName;

                if (playerId.equals(newHostId)) {
                    eventData.put("message", "당신이 새로운 방장입니다.");
                    eventData.put("isHost", true);
                    eventName = "host-assigned";
                } else {
                    // 다른 사용자들에게 새 방장 정보 알림
                    eventData.put("message", "새로운 방장은 " + newHostName + "입니다.");
                    eventData.put("hostId", newHostId);
                    eventName = "host-changed";
                }

                // 특정 사용자에게 이벤트 전송
                gameSseService.sendToSpecificPlayer(room.getRoomId(), playerId, eventName, eventData);
            });
        }

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
