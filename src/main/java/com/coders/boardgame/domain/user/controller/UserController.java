package com.coders.boardgame.domain.user.controller;

import com.coders.boardgame.domain.user.dto.UserDto;
import com.coders.boardgame.domain.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody UserDto userDto, HttpServletRequest request) {
        try {
            // 1. UserService를 통해 사용자 등록 및 세션 생성
            String sessionId = userService.signUp(userDto, request);

            // 2. 성공 응답 반환
            return ResponseEntity.ok(sessionId);
        } catch (Exception e) {
            // 3. 예외 처리
            return ResponseEntity.status(500).body("An error occurred during signup: " + e.getMessage());
        }
    }

    /**
     * 세션 유효성 확인 API
     * @param request HttpServletRequest
     * @return 세션 유효 상태를 반환
     */
    @GetMapping("/session")
    public ResponseEntity<String> checkSessionStatus(HttpServletRequest request){

        // HttpSession에서 세션 확인
        HttpSession session = request.getSession(false); // 세션이 없으면 null 반환

        if (session != null && session.getAttribute("userId") != null) {
            return ResponseEntity.ok("로그인 완료");
        } else {
            return ResponseEntity.status(401).body("로그인이 되어있지 않습니다.");
        }

    }
}