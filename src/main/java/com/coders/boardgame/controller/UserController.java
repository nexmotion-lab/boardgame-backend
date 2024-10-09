package com.coders.boardgame.controller;

import com.coders.boardgame.dto.UserDto;
import com.coders.boardgame.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody @Valid UserDto userDto) {
        userService.signUp(userDto);

        return ResponseEntity.ok("회원가입 성공");
    }
}
