package com.coders.boardgame.domain.user.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

    /**
     *
     * 세션으로 부터 userId 반환
     * @param request
     * @return userID
     */
    public Long getUserIdFromSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new IllegalStateException("세션이 유효하지 않습니다.");
        }

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new IllegalStateException("세션에 userId가 없습니다.");
        }

        return userId;
    }
}
