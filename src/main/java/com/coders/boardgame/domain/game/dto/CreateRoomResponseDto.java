package com.coders.boardgame.domain.game.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomResponseDto {
    private String roomId; // 생성된 방 ID
    private String roomName; // 방이름
    private int totalPlayers; // 최대 플레이어 수
    private PlayerDto host; // 방장 정보
}


