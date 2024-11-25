package com.coders.boardgame.entity;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Embeddable
@EqualsAndHashCode
public class SelectedOptionId implements Serializable {
    private Integer surveyId;
    private Long surveyResultId;

    public SelectedOptionId() {}

    public SelectedOptionId(Integer surveyId, Long surveyResultId) {
        this.surveyId = surveyId;
        this.surveyResultId = surveyResultId;
    }
}
