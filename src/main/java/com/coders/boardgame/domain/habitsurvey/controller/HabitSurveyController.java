package com.coders.boardgame.domain.habitsurvey.controller;

import com.coders.boardgame.domain.habitsurvey.entity.HabitSurvey;
import com.coders.boardgame.domain.habitsurvey.service.HabitSurveyService;
import com.coders.boardgame.domain.habitsurvey.dto.SurveySelectedOptionDto;
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
    public List<HabitSurvey> getAllSurveys() {
        return habitSurveyService.getAllSurveys();
    }

    @PostMapping("/results")
    public ResponseEntity<Void> saveSurveyResult(@RequestParam Long userId, @RequestBody List<SurveySelectedOptionDto> selectedOptions) {
        habitSurveyService.saveSurveyResult(userId, selectedOptions);
        return ResponseEntity.ok().build();
    }
}
