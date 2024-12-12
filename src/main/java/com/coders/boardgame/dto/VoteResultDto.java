package com.coders.boardgame.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class VoteResultDto {
    private boolean success;
    private List<String> disagreePlayers;
    private int agreeCount;
    private int disagreeCount;
    private Boolean isEarned;
}
