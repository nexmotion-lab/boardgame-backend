package com.coders.boardgame.domain.habitsurvey.dto;

import lombok.Data;

import java.util.List;

@Data
public class HabitSurveyResultDto {
    private List<HabitSurveySelectedOptionDto> selectedOptions;
    private Byte totalScore;
}
