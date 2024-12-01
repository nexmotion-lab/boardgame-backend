package com.coders.boardgame.domain.typetest.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestResultResponse {

    private Byte suhatTypeId;
    private String name;
    private String description;
    private String suggestion;

}
