package com.coders.boardgame.domain.habitsurvey.dto;

import lombok.Data;

@Data
public class SurveySelectedOptionDto {

    private Integer surveyId;
    private Long surveyResultId;
    private Byte score;
}
