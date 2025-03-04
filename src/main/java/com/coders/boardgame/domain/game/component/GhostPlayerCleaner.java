package com.coders.boardgame.domain.game.component;

import com.coders.boardgame.domain.game.dto.GameRoomDto;
import com.coders.boardgame.domain.game.dto.PlayerDto;
import com.coders.boardgame.domain.game.service.GameRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class GhostPlayerCleaner {

    private final GameRoomService gameRoomService;

    private static final long GHOST_THRESHOLD = 180_000; // 3분

    @Scheduled(fixedDelay = 30_000)
    public void removeGhostUser() {
        long now = System.currentTimeMillis();
        List<GameRoomDto> allRooms = gameRoomService.getAllRooms();

        for (GameRoomDto room : allRooms) {
            for (PlayerDto player : new ArrayList<>((room.getPlayers().values()))) {
                long diff = now - player.getLastPingTime();
                if (diff > GHOST_THRESHOLD) {
                    // 유령 유저로 판단
                    log.warn("유령 유저 감지: room={}, player={}, diff={}", room.getRoomId(), player.getPlayerId(), diff);
                    gameRoomService.leaveRoom(room.getRoomId(), player.getPlayerId(), false);
                }
            }
        }
    }
}
