    package com.coders.boardgame.domain.typetest.entity;

    import jakarta.persistence.*;
    import lombok.*;

    import java.time.LocalDateTime;

    @Entity
    @Table(name = "test_results")
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public class TestResult {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "user_id", nullable = false) // 외래 키를 단순 필드로 선언
        private Long userId;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "suhat_type_id", nullable = false)
        private SuhatType suhatType;

        @Column(name = "test_date", nullable = false)
        private LocalDateTime testDate;

    }
