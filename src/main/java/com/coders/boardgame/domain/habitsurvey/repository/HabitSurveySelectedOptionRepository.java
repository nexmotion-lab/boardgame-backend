package com.coders.boardgame.domain.habitsurvey.repository;

import com.coders.boardgame.domain.habitsurvey.entity.HabitSurveySelectedOption;
import com.coders.boardgame.domain.habitsurvey.entity.SelectedOptionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitSurveySelectedOptionRepository extends JpaRepository<HabitSurveySelectedOption, SelectedOptionId> {
}
