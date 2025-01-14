package com.coders.boardgame.domain.game.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitingRoomDto {
    private String roomId;              // 방 ID
    private String roomName;            // 방 이름
    private int currentPlayers;         // 현재 인원
    private int totalPlayers;           // 최대 인원
    private List<PlayerDto> players;    // 참가자 정보 리스트
    private Long hostId; // 호스트 id
}