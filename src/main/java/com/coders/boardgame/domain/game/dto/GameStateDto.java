package com.coders.boardgame.domain.game.dto;

import com.coders.boardgame.domain.game.enums.GamePhase;
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
    private int currentRound;
    private Long hostId;
    private List<PlayerDto> players;
    private GamePhase currentPhase;
    private int assignedPictureCardId;
    private int assignedTextCardId;
}
