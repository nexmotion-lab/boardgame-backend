package com.coders.boardgame.domain.game.dto;

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
