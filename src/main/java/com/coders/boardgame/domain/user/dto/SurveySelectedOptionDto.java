package com.coders.boardgame.dto;

import lombok.Data;

@Data
public class SurveySelectedOptionDto {

    private Integer surveyId;
    private Long surveyResultId;
    private Byte score;
}
