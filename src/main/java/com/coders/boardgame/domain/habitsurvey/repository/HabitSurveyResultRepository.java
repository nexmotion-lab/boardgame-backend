package com.coders.boardgame.domain.habitsurvey.repository;

import com.coders.boardgame.domain.habitsurvey.entity.HabitSurveyResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HabitSurveyResultRepository extends JpaRepository<HabitSurveyResult, Long> {

    @Query("SELECT h.totalScore FROM HabitSurveyResult h WHERE h.user.id = :userId ORDER BY h.surveyDate DESC")
    Byte findTotalScoreByUserId(@Param("userId") Long userId);
}