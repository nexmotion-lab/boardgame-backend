package com.coders.boardgame.entity;

import com.coders.boardgame.dto.UserDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
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
    private String name;

    @Column(nullable = false)
    private int gender;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public User(UserDto userDto) {

        this.school = userDto.getSchool();
        this.name = userDto.getName();
        this.gender = userDto.getGender();
        this.createdAt = LocalDateTime.now();
    }
}
