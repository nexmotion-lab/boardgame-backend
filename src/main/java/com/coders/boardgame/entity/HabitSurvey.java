package com.coders.boardgame.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "habit_surveys")
@Data
public class HabitSurvey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT 설정
    private Integer id;

    private String content; // content 컬럼과 매핑

}
