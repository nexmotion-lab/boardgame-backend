package com.coders.boardgame.domain.typetest.controller;

import com.coders.boardgame.domain.typetest.dto.TestResultResponse;
import com.coders.boardgame.domain.typetest.dto.TestResultRequest;
import com.coders.boardgame.domain.typetest.service.TestResultService;
import com.coders.boardgame.domain.user.service.SessionService;
import com.coders.boardgame.domain.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test-results")
@RequiredArgsConstructor
public class TestResultController {

    private final TestResultService testResultService;
    private final SessionService sessionService;

    /**
     *
     * @param requestDto 유형검사 정보
     * @return suhat 유형
     */
    @PostMapping
    public ResponseEntity<TestResultResponse> saveTestResult(@RequestBody TestResultRequest requestDto, HttpServletRequest request) {
        // userSessionService를 통해 userid 가져오기
        Long userId = sessionService.getUserIdFromSession(request);

        TestResultResponse response = testResultService.saveTestResult(requestDto, userId);
        return ResponseEntity.ok(response);
    }
}
