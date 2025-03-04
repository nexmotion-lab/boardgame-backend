package com.coders.boardgame.domain.game.service;

import com.coders.boardgame.domain.game.dto.ConnectionResult;
import com.coders.boardgame.domain.game.event.PlayerDisconnectedEvent;
import com.coders.boardgame.domain.game.event.PlayerReadyCanceledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameSseService {

    private static final Long SSE_SESSION_TIMEOUT = 60 * 120 * 1000L;
    private final Map<String, Map<Long, SseEmitter>> sseEmitters = new ConcurrentHashMap<>();
    private final ApplicationEventPublisher eventPublisher; // 이벤트 발행기 주입


    /**
     * SSE 연결 추가
     * @param roomId 방 ID
     * @param playerId 플레이어 ID
     * @return ConnectionResult
     */
    public ConnectionResult connectToRoom(String roomId, Long playerId) {

        Map<Long, SseEmitter> roomEmitters = sseEmitters.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());

        boolean isReconnecting = false;
        SseEmitter existingEmitter = roomEmitters.get(playerId);
        if (existingEmitter != null) {
            // 기존 연결이 존재하면 제거하고 재연결 처리
            existingEmitter.complete();
            isReconnecting = true;
            log.info("플레이어 재연결: roomId={}, playerId={}", roomId, playerId);
        }

        SseEmitter emitter = new SseEmitter(SSE_SESSION_TIMEOUT); // 타임아웃 2시간
        log.info("재연결 {} , emitter 해쉬코드 {}", isReconnecting,emitter.hashCode());
        roomEmitters.put(playerId, emitter);

        // 현재 emitter 인스턴스를 final 변수에 캡쳐
        final SseEmitter currentEmitter = emitter;

        emitter.onCompletion(() -> handleDisconnection(roomId, "completion", playerId, false, currentEmitter));
        emitter.onTimeout(() -> handleDisconnection(roomId, "timeout", playerId, true, currentEmitter));
        emitter.onError(e -> {
            if (e instanceof IOException) {
                // 클라이언트 끊김
                log.debug("SSE onError: 클라이언트 끊김, roomId={}, playerId={}, msg={}", roomId, playerId, e.getMessage());
            } else {
                // 정말 예상치 못한 에러라면 error 로깅
                log.error("SSE onError 발생: {}", e.getMessage(), e);
            }
            handleDisconnection(roomId, "error", playerId, true, currentEmitter);
        });

        log.info("플레이어{} : 방과 sse 연결 성공", playerId);

        return new ConnectionResult(emitter, isReconnecting);
    }


    /**
     * SSE로 방에 있는 전체 인원들한테 event 전송
     * @param roomId 방 ID
     * @param eventName 이벤트 이름
     * @param data 이벤트 데이터
     */
    public void sendRoomEvent(String roomId, String eventName, Object data){
        Map<Long, SseEmitter> roomEmitters = sseEmitters.get(roomId);

        if (roomEmitters != null) {
            roomEmitters.forEach((playerId, emitter) ->{
                try {
                    emitter.send(SseEmitter.event()
                            .name(eventName)
                            .data(data));
                } catch (IOException e){
                    roomEmitters.remove(playerId);
                    log.debug("플레이어 이벤트 전송 실패: roomId={}, playerId={}, eventName={}, error={}",
                            roomId, playerId, eventName, e.getMessage());
                }
            });
        }
    }

    /**
     * 방에 자신을 제외한 모든 인원한테 이벤트 전송
     * @param roomId 방 Id
     * @param eventName 이벤트 이름
     * @param data 이벤트 데이터
     * @param excludePlayerId 자신의 playerId
     */
    public void sendRoomEventToOthers(String roomId, String eventName, Object data, Long excludePlayerId){
        Map<Long, SseEmitter> roomEmitters = sseEmitters.get(roomId);

        if (roomEmitters != null) {
            roomEmitters.forEach((playerId, emitter) ->{
                if (!playerId.equals(excludePlayerId)) {
                    try {
                        emitter.send(SseEmitter.event()
                                .name(eventName)
                                .data(data));
                    } catch (IOException e) {
                        roomEmitters.remove(playerId);
                        log.debug("플레이어 이벤트 전송 실패: roomId={}, playerId={}, eventName={}, error={}",
                                roomId, playerId, eventName, e.getMessage());
                    }
                }
            });
        }
    }

    /**
     * 특정 클라이언트한테 sse 이벤트 전송
     * @param roomId 방 id
     * @param playerId 플레이어 id
     * @param eventName 이벤트 이름
     * @param data 이벤트 데이터
     */
    public void sendToSpecificPlayer(String roomId, Long playerId, String eventName, Object data){
        Map<Long, SseEmitter> roomEmitters = sseEmitters.get(roomId);
        if (roomEmitters != null){
            SseEmitter emitter = roomEmitters.get(playerId);
            if (emitter != null) {
                try {
                    emitter.send(SseEmitter.event()
                            .name(eventName)
                            .data(data));
                } catch (IOException e){
                    roomEmitters.remove(playerId);
                    log.debug("특정 사용자 이벤트 전송 실패: roomId={}, playerId={}, eventName={}, error={}",
                            roomId, playerId, eventName, e.getMessage());                }
            }
        }
    }
    /**
     * SSE 연결 모두 제거
     * @param roomId
     */
    public void removeEmitters(String roomId) {
        Map<Long, SseEmitter> roomEmitters = sseEmitters.remove(roomId);
        if (roomEmitters != null) {
            roomEmitters.values().forEach(SseEmitter::complete);
        }
    }

    /**
     * 사용자 나갈 시 연결 종료
     * @param roomId
     * @param reason
     * @param playerId
     */
    public void disconnectPlayer(String roomId, String reason, Long playerId, boolean isUnexpected){
        handleDisconnection(roomId, reason, playerId, isUnexpected, null); // 의도적인 연결 종료
    }

    /**
     * SSE 연결 종료, 타임아웃, 오류 처리
     *
     * @param roomId   방 ID
     * @param playerId 플레이어 ID
     * @param reason   종료 이유
     * @param isUnexpected 의도적 연결 끊김인지
     * @param emitter 끊으려는 emitter
     */
    private void handleDisconnection(String roomId, String reason, Long playerId, boolean isUnexpected, SseEmitter emitter) {

        Map<Long, SseEmitter> roomEmitters = sseEmitters.get(roomId);
        if (roomEmitters == null) {
            return;
        }

        // emitter가 전달되었다면, 현재 등록된 emitter와 비교해서 다르면 그냥 종료
        if (emitter != null) {
            SseEmitter currentEmitter = roomEmitters.get(playerId);
            if (currentEmitter != emitter) {
                return;
            }
        }

        SseEmitter removedEmitter = roomEmitters.remove(playerId);
        if (removedEmitter != null) {
            log.info("handleDisconnection - emitter 제거, 해쉬코드 {}", removedEmitter.hashCode());
            removedEmitter.complete();
        }

        log.info("SSE 연결 해제 {}: roomId = {}, playerId = {}", reason, roomId, playerId);

        if (isUnexpected) {
            // 비의도적인 연결 끊김 시 이벤트 발행
            eventPublisher.publishEvent(new PlayerDisconnectedEvent(this, roomId, playerId, true));
        }
    }
}
