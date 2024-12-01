package com.coders.boardgame.domain.user.controller;

import com.coders.boardgame.domain.user.dto.UserDto;
import com.coders.boardgame.domain.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody @Valid UserDto userDto, HttpServletRequest request) {
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
}
