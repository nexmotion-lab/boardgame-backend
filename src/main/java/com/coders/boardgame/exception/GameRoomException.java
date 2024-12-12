package com.coders.boardgame.exception;

import org.springframework.http.HttpStatus;

public class GameRoomException extends RuntimeException {
    private final HttpStatus status;

    public GameRoomException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
