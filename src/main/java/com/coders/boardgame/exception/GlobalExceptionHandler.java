package com.coders.boardgame.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.security.core.AuthenticationException;

import java.net.URI;


@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAllExceptions(Exception ex, WebRequest request) {
        log.warn("다루지 않는 에러: {}", ex.getMessage(), ex);

        // SSE 요청인 경우 간단한 메시지 반환 (또는 빈 응답)
        if (isSseRequest(request)) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("SSE 연결 오류");
        }

        ProblemDetail problemDetail = createProblemDetail(
                "Internal Server Error",
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage(),
                "500",
                request
        );

        return new ResponseEntity<>(problemDetail, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());

        ProblemDetail problemDetail = createProblemDetail(
                "Bad Request",
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                "400",
                request
        );

        return new ResponseEntity<>(problemDetail, HttpStatus.BAD_REQUEST);

    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());

        ProblemDetail problemDetail = createProblemDetail(
                "권한 에러",
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                "40101",
                request
        );

        return new ResponseEntity<>(problemDetail, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(GameRoomException.class)
    public ResponseEntity<String> handleGameRoomException(GameRoomException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ex.getMessage());
    }

    /**
     * 공통 ProblemDetail 생성 메서드
     */
    private ProblemDetail createProblemDetail(String title, HttpStatus status, String detail, String errorCode, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setInstance(URI.create(request.getDescription(false)));
        problemDetail.setTitle(title);
        problemDetail.setProperty("errorCode", errorCode);
        return problemDetail;
    }

    /**
     * sse요청일떄,
     * @param request
     * @return
     */
    private boolean isSseRequest(WebRequest request) {
        String acceptHeader = request.getHeader("Accept");
        return acceptHeader != null && acceptHeader.contains("text/event-stream");
    }

}
