package com.coders.boardgame.repository;

import com.coders.boardgame.entity.HabitSurveyResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitSurveyResultRepository extends JpaRepository<HabitSurveyResult, Long> {
}