package com.coders.boardgame.domain.habitsurvey.entity;

import com.coders.boardgame.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "habit_survey_results")
@NoArgsConstructor
@AllArgsConstructor
@Getter @Builder
public class HabitSurveyResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "survey_date")
    private LocalDateTime surveyDate;

}
