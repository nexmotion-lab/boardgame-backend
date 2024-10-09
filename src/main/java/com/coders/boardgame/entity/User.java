package com.coders.boardgame.entity;

import com.coders.boardgame.dto.UserDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String school;

    @Column(nullable = false)
    private int grade;

    @Column(name = "class", nullable = false)
    private int userClass;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int gender;

    public User(UserDto userDto) {
        this.school = userDto.getSchool();
        this.grade = userDto.getGrade();
        this.userClass = userDto.getUserClass();
        this.name = userDto.getName();
        this.gender = userDto.getGender();
    }
}
