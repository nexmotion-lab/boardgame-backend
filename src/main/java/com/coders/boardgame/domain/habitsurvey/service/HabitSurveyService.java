package com.coders.boardgame.domain.habitsurvey.service;

import com.coders.boardgame.domain.habitsurvey.entity.HabitSurvey;
import com.coders.boardgame.domain.habitsurvey.repository.HabitSurveyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HabitSurveyService {


    private final HabitSurveyRepository habitSurveyRepository;

    public List<HabitSurvey> getAllSurveys() {
        return habitSurveyRepository.findAll(); // 모든 데이터 조회
    }
}
