package com.coders.boardgame.exception.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CustomSessionAuthenticationException extends ResponseStatusException {
    public CustomSessionAuthenticationException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);

    }
}
