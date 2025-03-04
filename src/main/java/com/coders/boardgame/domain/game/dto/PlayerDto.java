package com.coders.boardgame.domain.game.dto;

import com.coders.boardgame.domain.user.dto.UserDto;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlayerDto {
    private Long playerId;
    private int avatarId;
    private UserDto playerInfo; // 사용자 정보 (name, gender, school 포함)
    private int sequenceNumber; // 게임 내 순번
    private int collectedPuzzlePieces; // 플레이어가 모은 퍼즐 조각 수
    private boolean isSpeaking; // 현재 말하기 진행 여부
    private int usageTime; // 스마트폰 이용시간(분)
    private int surveyScore; // 이용습관 진단지 점수
    private boolean isReady; // 준비완료 했는지 확인
    private int seatIndex; // 자리 번호
    private long lastPingTime; // 마지막 ping 받은 시각 (밀리초)
}
