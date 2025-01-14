package com.coders.boardgame.domain.game.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 연결 결과를 나타내는 레코드
 */
public record ConnectionResult(SseEmitter emitter, boolean isReconnecting) {
}
