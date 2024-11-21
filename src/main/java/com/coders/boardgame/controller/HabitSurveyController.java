package com.coders.boardgame.controller;

import com.coders.boardgame.entity.HabitSurvey;
import com.coders.boardgame.repository.HabitSurveyRepository;
import com.coders.boardgame.service.HabitSurveyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/surveys")
@RequiredArgsConstructor
@Slf4j
public class HabitSurveyController {

    private final HabitSurveyService habitSurveyService;

    @GetMapping
    public List<HabitSurvey> getAllSurveys(HttpServletRequest request) {
        String sessionId = request.getHeader("X-session-ID");
        log.info("Session ID: " + sessionId);
        return habitSurveyService.getAllSurveys();
    }

}
