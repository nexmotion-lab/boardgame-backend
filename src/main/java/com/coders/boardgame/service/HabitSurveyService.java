package com.coders.boardgame.service;

import com.coders.boardgame.entity.HabitSurvey;
import com.coders.boardgame.repository.HabitSurveyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
