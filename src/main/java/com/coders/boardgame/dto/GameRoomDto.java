package com.coders.boardgame.dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class GameRoomDto {
    private String roomId;
    private List<PlayerDto> players = new ArrayList<>();
    private List<String> disagreePlayers = new ArrayList<>();
    private int headCount;
    private int currentTurn = 0;
    private int count = 0;
    private int agreeCount = 0;
    private int totalPuzzlePiece = 0;
}
