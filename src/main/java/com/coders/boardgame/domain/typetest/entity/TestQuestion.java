package com.coders.boardgame.domain.typetest.entity;


import com.coders.boardgame.domain.typetest.entity.ServiceType;
import com.coders.boardgame.domain.typetest.entity.TestOption;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "test_questions")
@NoArgsConstructor
@AllArgsConstructor
public class TestQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT 설정
    private Integer id;

    @Column(nullable = false)
    private String content; // content 컬럼과 매핑

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_type_id", nullable = false)
    private ServiceType serviceType;

    @OneToMany(mappedBy = "testQuestion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TestOption> testOptions = new ArrayList<>();

    public void addTestOption(TestOption option) {
        testOptions.add(option);
        option.setTestQuestion(this);
    }
}
