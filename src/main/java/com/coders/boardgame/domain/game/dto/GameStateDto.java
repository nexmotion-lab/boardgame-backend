package com.coders.boardgame.domain.game.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GameStateDto {
    private String roomId;
    private String roomName;
    private int totalPlayers;
    private int totalPuzzlePieces;
    private int currentPuzzlePieces;
    private int currentTurn;
    private List<PlayerDto> players;
}
