package com.coders.boardgame.domain.habitsurvey.service;

import com.coders.boardgame.domain.habitsurvey.entity.HabitSurvey;
import com.coders.boardgame.domain.habitsurvey.entity.HabitSurveyResult;
import com.coders.boardgame.domain.habitsurvey.entity.HabitSurveySelectedOption;
import com.coders.boardgame.domain.habitsurvey.entity.SelectedOptionId;
import com.coders.boardgame.domain.habitsurvey.repository.HabitSurveyRepository;
import com.coders.boardgame.domain.user.entity.User;
import com.coders.boardgame.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.coders.boardgame.domain.habitsurvey.dto.SurveySelectedOptionDto;
import com.coders.boardgame.domain.habitsurvey.repository.HabitSurveyResultRepository;
import com.coders.boardgame.domain.habitsurvey.repository.HabitSurveySelectedOptionRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HabitSurveyService {


    private final HabitSurveyRepository habitSurveyRepository;
    private final HabitSurveyResultRepository habitSurveyResultRepository;
    private final HabitSurveySelectedOptionRepository habitSurveySelectedOptionRepository;
    private final UserRepository userRepository;

    public List<HabitSurvey> getAllSurveys() {
        return habitSurveyRepository.findAll();
    }

    public void saveSurveyResult(Long userId, List<SurveySelectedOptionDto> selectedOptions) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾지 못했습니다."));

        HabitSurveyResult surveyResult = HabitSurveyResult.builder()
                        .user(user)
                        .surveyDate(LocalDateTime.now())
                .build();
        habitSurveyResultRepository.save(surveyResult);

        List<HabitSurveySelectedOption> selectedOptionToSave = new ArrayList<>();
        for(SurveySelectedOptionDto option : selectedOptions) {
            HabitSurvey habitSurvey = habitSurveyRepository.findById(option.getSurveyId())
                    .orElseThrow(() -> new RuntimeException("질문을 찾을 수 없습니다."));

            HabitSurveySelectedOption selectedOption = HabitSurveySelectedOption.builder()
                    .id(new SelectedOptionId(habitSurvey.getId(), surveyResult.getId()))
                    .survey(habitSurvey)
                    .surveyResult(surveyResult)
                    .score(option.getScore())
                    .build();

            selectedOptionToSave.add(selectedOption);
        }

        habitSurveySelectedOptionRepository.saveAll(selectedOptionToSave);
    }
}
