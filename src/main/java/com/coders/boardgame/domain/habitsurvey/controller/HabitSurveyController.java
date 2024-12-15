package com.coders.boardgame.domain.habitsurvey.controller;

import com.coders.boardgame.domain.habitsurvey.dto.HabitSurveyResultDto;
import com.coders.boardgame.domain.habitsurvey.entity.HabitSurvey;
import com.coders.boardgame.domain.habitsurvey.service.HabitSurveyService;
import com.coders.boardgame.exception.auth.CustomSessionAuthenticationException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/surveys")
@RequiredArgsConstructor
@Slf4j
public class HabitSurveyController {

    private final HabitSurveyService habitSurveyService;

    @GetMapping
    public ResponseEntity<List<HabitSurvey>> getAllSurveys() {
        List<HabitSurvey> surveyList = habitSurveyService.getAllSurveys();
        return ResponseEntity.ok(surveyList);
    }

    @PostMapping("/results")
    public ResponseEntity<Void> saveSurveyResult(@RequestBody HabitSurveyResultDto habitSurveyResult, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            log.warn("사용자 ID가 없습니다. userId: " + userId);
            throw new CustomSessionAuthenticationException("사용자 ID가 없습니다.");
        }

        habitSurveyService.saveSurveyResult(userId, habitSurveyResult);
        return ResponseEntity.ok().build();
    }
}
