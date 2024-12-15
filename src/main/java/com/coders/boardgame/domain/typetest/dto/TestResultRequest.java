package com.coders.boardgame.domain.typetest.dto;

import com.coders.boardgame.domain.typetest.entity.TestOption;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestResultRequest {
    private Byte serviceTypeId;
    private Byte totalScore;
    private List<Integer> selectedOptionIds;

}
