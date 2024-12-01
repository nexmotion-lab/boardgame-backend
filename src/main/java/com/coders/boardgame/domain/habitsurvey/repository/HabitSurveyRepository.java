package com.coders.boardgame.domain.habitsurvey.repository;

import com.coders.boardgame.domain.habitsurvey.entity.HabitSurvey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitSurveyRepository extends JpaRepository<HabitSurvey, Integer> {
}
