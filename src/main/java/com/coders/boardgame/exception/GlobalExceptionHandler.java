package com.coders.boardgame.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(GameRoomException.class)
    public ResponseEntity<String> handleGameRoomException(GameRoomException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ex.getMessage());
    }
}
