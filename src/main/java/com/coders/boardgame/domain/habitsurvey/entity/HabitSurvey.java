package com.coders.boardgame.domain.habitsurvey.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "habit_surveys")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HabitSurvey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT 설정
    private Integer id;

    private String content; // content 컬럼과 매핑

}
