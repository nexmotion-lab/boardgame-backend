package com.coders.boardgame.domain.game.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameSseService {

    private final Map<String, Map<Long, SseEmitter>> sseEmitters = new ConcurrentHashMap<>();


    /**
     * SSE 연결 추가
     * @param roomId 방 ID
     * @param playerId 플레이어 ID
     * @return SseEmitter
     */
    public SseEmitter connectToRoom(String roomId, Long playerId) {
        SseEmitter emitter = new SseEmitter(0L); // 타임아웃 없음
        sseEmitters.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(playerId, emitter);

        emitter.onCompletion(() -> handleDisconnection(roomId, "completion", playerId));
        emitter.onTimeout(() -> handleDisconnection(roomId, "timeout", playerId));
        emitter.onError(e -> handleDisconnection(roomId, "error", playerId));

        return emitter;
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
                    log.error("플레이어 이벤트 전송 실패: roomId={}, playerId={}, eventName={}, error={}",
                            roomId, playerId, eventName, e.getMessage());
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
                    log.error("특정 사용자 이벤트 전송 실패: roomId={}, playerId={}, eventName={}, error={}",
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

    public void disconnectPlayer(String roomId, String reason, Long playerId){
        handleDisconnection(roomId, reason, playerId);
    }

    /**
     * SSE 연결 종료, 타임아웃, 오류 처리
     * @param roomId 방 ID
     * @param playerId 플레이어 ID
     * @param reason 종료 이유
     */
    private void handleDisconnection(String roomId, String reason, Long playerId) {
        Map<Long, SseEmitter> roomEmitters = sseEmitters.get(roomId);
        if (roomEmitters != null) {
            SseEmitter emitter = roomEmitters.remove(playerId);
            if (emitter!= null){
                emitter.complete(); // 연결 종료
            }
        }
        log.info("SSE 연결 해제 {}: roomId = {}, playerId = {}", reason, roomId, playerId);
    }
}
