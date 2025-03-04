package com.coders.boardgame.domain.game.dto;

import com.coders.boardgame.domain.user.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinRoomRequestDto {
    private UserDto userInfo; // 사용자 정보
    private int surveyScore; // 설문지 정보
    private int avatarMaxId; // 가지고 있는 아바타 아이콘 maxId
}
