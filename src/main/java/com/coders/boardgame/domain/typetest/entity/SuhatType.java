package com.coders.boardgame.domain.typetest.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "suhat_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuhatType {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Byte id;

    @Column(nullable = false, length = 40)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String suggestion;

    @Column(name = "min_score", nullable = false)
    private Byte minScore;

    @Column(name = "max_score", nullable = false)
    private Byte maxScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_type_id", nullable = false)
    private ServiceType serviceType;


}
