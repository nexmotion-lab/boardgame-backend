package com.coders.boardgame.domain.game.service;

import com.coders.boardgame.domain.game.dto.*;
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
                .roomStatus(GameRoomDto.RoomStatus.WAITING)
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

        // 게임 SSE 서비스에서 연결 시도 및 재연결 여부 확인
        ConnectionResult connectionResult = gameSseService.connectToRoom(roomId, playerId);
        SseEmitter emitter = connectionResult.emitter();
        boolean isReconnecting = connectionResult.isReconnecting();

        // 방 상태에 따른 처리
        switch (room.getRoomStatus()){
            case ENDED:
                // 게임이 종료 된 경우 "game-ended" 이벤트 전송
                try {
                    emitter.send(SseEmitter.event()
                            .name("game-ended")
                            .data("게임이 종료되었습니다. 대기방으로 이동합니다.")
                    );
                } catch (IOException e) {
                    log.error("게임 종료 알림 전송 실패: roomId={}, playerId={}, error={}", roomId, playerId, e.getMessage());
                    emitter.completeWithError(e);
                }
                return emitter;

            case WAITING:
                // 대기방 상태일 때 연결 처리
                WaitingRoomDto waitingRoomDto = WaitingRoomDto.builder()
                        .roomId(room.getRoomId())
                        .roomName(room.getRoomName())
                        .currentPlayers(room.getCurrentPlayers().get())
                        .totalPlayers(room.getTotalPlayers())
                        .hostId(room.getHostId())
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
                        .totalPlayers(room.getTotalPlayers())
                        .totalPuzzlePieces(room.getTotalPuzzlePieces())
                        .currentPuzzlePieces(room.getCurrentPuzzlePieces())
                        .currentTurn(room.getCurrentTurn())
                        .currentRound(room.getCurrentRound())
                        .players(new ArrayList<>(room.getPlayers().values()))
                        .build();

                try {
                    emitter.send(SseEmitter.event()
                            .name("game-state")
                            .data(gameState)
                    );
                } catch (IOException e) {
                    log.error("게임 상태 전송 실패: roomId={}, playerId={}, error={}", roomId, playerId, e.getMessage());
                    emitter.completeWithError(e);
                }

                if (isReconnecting) {
                    gameSseService.sendRoomEventToOthers(roomId, "player-reconnected", room.getPlayers().get(playerId), playerId);
                } else {
                    gameSseService.sendRoomEventToOthers(roomId, "player-joined", room.getPlayers().get(playerId), playerId);
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


        return WaitingRoomDto.builder()
                .roomId(roomId)
                .roomName(room.getRoomName())
                .currentPlayers(currentPlayers)
                .totalPlayers(room.getTotalPlayers())
                .hostId(room.getHostId())
                .players(new ArrayList<>(room.getPlayers().values()))
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

        if (room.getRoomStatus() == GameRoomDto.RoomStatus.IN_GAME) {
            log.info("IN_GAME 상태에서 플레이어 {}가 나갔으므로 게임 종료 진행", playerId);
            String reason = removedPlayer.getPlayerInfo().getName() + "가 나갔습니다.";
            endGameAndMoveToWaitingRoom(roomId, reason);

        } else if (room.getRoomStatus() == GameRoomDto.RoomStatus.WAITING) {
            // 방장이 나간 경우 새로운 방장 무작위로 선정
            if (room.getHostId().equals(playerId) && currentPlayers > 0) {
                assignNewHost(room, true);
            }
            // 방 상태 최신화 전송
            if (currentPlayers > 0) {
                gameSseService.sendRoomEventToOthers(room.getRoomId(), "player-left", playerId, playerId);
            } else {
                deleteRoom(roomId); // 방에 아무도 없으면 삭제
            }
        } else if (room.getRoomStatus() == GameRoomDto.RoomStatus.ENDED) {
            if (currentPlayers <=0){
                deleteRoom(roomId);
            }
        }

        if (isIntentional){
            gameSseService.disconnectPlayer(roomId, "player-left", playerId, false);
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
    public void endGameAndMoveToWaitingRoom(String roomId, String reason){
        GameRoomDto room = gameRooms.get(roomId);

        // 게임이 이미 종료 된 경우 중복 실행 방지
        if (room.getRoomStatus() != GameRoomDto.RoomStatus.ENDED) {
            // 방상태 ended로 변환
            room.setRoomStatus(GameRoomDto.RoomStatus.ENDED);

            Map<String, String> eventData = Map.of(
                    "reason", reason,
                    "data", "게임이 종료되어 잠시 후 대기방으로 이동합니다"
            );

            // 모든 플레이어에게 게임 종료 이벤트 전송
            gameSseService.sendRoomEvent(roomId, "game-ended", eventData);

            // gameEvent 발행
            applicationEventPublisher.publishEvent(new GameEndedEvent(this, roomId));

            log.info("게임이 종료되었습니다: roomId={}", roomId);
        } else {
            log.info("이미 게임이 종료되었습니다. roomId={}", roomId);
        }
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
