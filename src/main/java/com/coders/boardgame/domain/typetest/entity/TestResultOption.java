package com.coders.boardgame.domain.typetest.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "test_result_options")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestResultOption {

    @EmbeddedId
    private TestResultOptionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("testResultId")
    @JoinColumn(name = "result_id", nullable = false)
    private TestResult testResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("testOptionId")
    @JoinColumn(name = "option_id", nullable = false)
    private TestOption testOption;

}
