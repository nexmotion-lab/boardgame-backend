package com.coders.boardgame.exception;

import com.coders.boardgame.exception.auth.CustomSessionAuthenticationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.net.URI;


@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleAllExceptions(Exception ex, WebRequest request) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);

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
                "Authentication Error",
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                "40101",
                request
        );

        return new ResponseEntity<>(problemDetail, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(CustomSessionAuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleSessionAuthenticationException(CustomSessionAuthenticationException ex, WebRequest request) {
        log.warn("Session authentication error: {}", ex.getReason());

        ProblemDetail problemDetail = createProblemDetail(
                "Session Error",
                HttpStatus.UNAUTHORIZED,
                ex.getReason(),
                "40102",
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

}
