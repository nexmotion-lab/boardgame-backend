package com.coders.boardgame.domain.habitsurvey.entity;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Embeddable
@EqualsAndHashCode
public class HabitSurveySelectedOptionId implements Serializable {
    private Integer surveyId;
    private Long surveyResultId;

    public HabitSurveySelectedOptionId() {}

    public HabitSurveySelectedOptionId(Integer surveyId, Long surveyResultId) {
        this.surveyId = surveyId;
        this.surveyResultId = surveyResultId;
    }
}
