package com.coders.boardgame.domain.typetest.dto;

import com.coders.boardgame.domain.typetest.dto.TestOptionDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TestQuestionDto {

    private Integer id;
    private String content;
    private List<TestOptionDto> testOptions;

}
