package com.coders.boardgame.repository;

import com.coders.boardgame.entity.HabitSurvey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitSurveyRepository extends JpaRepository<HabitSurvey, Long> {
}
