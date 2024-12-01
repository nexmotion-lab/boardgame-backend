package com.coders.boardgame.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PlayerDto {
    private Long userId;
    private String name;
    private byte score;
    private Integer usedTime = 0;
    private int puzzlePiece = 0;
    private boolean isSpeaker;
    private LocalDateTime startTime;
}
