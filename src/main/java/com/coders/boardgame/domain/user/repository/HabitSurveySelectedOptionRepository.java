package com.coders.boardgame.repository;

import com.coders.boardgame.entity.HabitSurveySelectedOption;
import com.coders.boardgame.entity.SelectedOptionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitSurveySelectedOptionRepository extends JpaRepository<HabitSurveySelectedOption, SelectedOptionId> {
}
