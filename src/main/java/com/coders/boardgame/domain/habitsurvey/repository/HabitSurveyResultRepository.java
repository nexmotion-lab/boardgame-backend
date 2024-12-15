package com.coders.boardgame.domain.habitsurvey.repository;

import com.coders.boardgame.domain.habitsurvey.entity.HabitSurveyResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitSurveyResultRepository extends JpaRepository<HabitSurveyResult, Long> {
}