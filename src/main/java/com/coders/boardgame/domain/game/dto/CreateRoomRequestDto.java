package com.coders.boardgame.domain.game.dto;

import com.coders.boardgame.domain.user.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CreateRoomRequestDto {
    private String roomName;
    private int totalPlayers; // 최대 플레이어 수 (3명 또는 4명)
    private UserDto hostInfo; // 방장 정보 포함
}
