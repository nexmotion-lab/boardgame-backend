package com.coders.boardgame.domain.typetest.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TestResultOptionId implements Serializable {
    private Long testResultId;
    private Integer testOptionId;

}
