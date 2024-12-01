package com.coders.boardgame.domain.typetest.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "test_options")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 40)
    private String content;

    @Column(nullable = false)
    private Byte score;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_question_id", nullable = false)
    private TestQuestion testQuestion;
}
