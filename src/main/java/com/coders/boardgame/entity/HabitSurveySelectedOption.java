package com.coders.boardgame.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "habit_survey_selected_options")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HabitSurveySelectedOption {

    @EmbeddedId
    private SelectedOptionId id;

    @ManyToOne
    @MapsId("surveyId")
    @JoinColumn(name = "survey_id")
    private HabitSurvey survey;

    @ManyToOne
    @MapsId("surveyResultId")
    @JoinColumn(name = "survey_result_id")
    private HabitSurveyResult surveyResult;

    @Column
    private int score;
}