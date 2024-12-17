package com.coders.boardgame.domain.game.service;

import com.coders.boardgame.domain.game.dto.GameRoomDto;
import com.coders.boardgame.domain.game.dto.PlayerDto;
import com.coders.boardgame.domain.game.dto.VoteResultDto;
import com.coders.boardgame.domain.habitsurvey.repository.HabitSurveyResultRepository;
import com.coders.boardgame.domain.user.entity.User;
import com.coders.boardgame.domain.user.repository.UserRepository;
import com.coders.boardgame.exception.GameRoomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final UserRepository userRepository;
    private final HabitSurveyResultRepository habitSurveyResultRepository;
    private final Map<String, GameRoomDto> gameRooms = new ConcurrentHashMap<>();

    public String generateRoomId() {
        int attempts = 0;
        int maxAttempts = 100;

        String roomId;
        do {
            if (attempts++ >= maxAttempts) {
                throw new IllegalStateException("Room ID 생성에 실패했습니다.");
            }
            roomId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        } while (gameRooms.containsKey(roomId));

        return roomId;
    }

    public String createGameRoom(Long userId, GameRoomDto room) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        PlayerDto createdUser = PlayerDto.builder()
                .userId(userId)
                .name(user.getName())
                .score(habitSurveyResultRepository.findTotalScoreByUserId(userId))
                .build();

        String roomId = generateRoomId();

        GameRoomDto newRoom = GameRoomDto.builder()
                .roomId(roomId)
                .roomName(room.getRoomName())
                .headCount(room.getHeadCount())
                .players(new ArrayList<>(List.of(createdUser)))
                .build();

        gameRooms.put(roomId, newRoom);

        return roomId;
    }

    public GameRoomDto getGameRoom(String roomId) {
        return gameRooms.get(roomId);
    }

    //fixme.
    //  나간 플레이어 반영, 이미 입장한 플레이어 중복 입장 불가 처리.
    public PlayerDto addPlayer(String roomId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        GameRoomDto room = gameRooms.get(roomId);

        if (room == null) {
            log.info("방이 존재하지 않습니다: " + roomId);
            throw new GameRoomException("방이 존재하지 않습니다: " + roomId, HttpStatus.NOT_FOUND);
        }

        if (room.getPlayers().size() >= room.getHeadCount()) {
            log.info("인원이 꽉 찬 방입니다: " + roomId);
            throw new GameRoomException("인원이 꽉 찬 방입니다: " + roomId, HttpStatus.FORBIDDEN);
        }

        PlayerDto player = PlayerDto.builder()
                .userId(userId)
                .name(user.getName())
                .score(habitSurveyResultRepository.findTotalScoreByUserId(userId))
                .build();

        room.getPlayers().add(player);
        log.info("플레이어 추가: " + player.getName() + " | 방 ID: " + roomId + " | 현재 플레이어 수: " + room.getPlayers().size());

        return player;
    }

    public void submitUsedTime(String roomId, PlayerDto submittedPlayer) {
        GameRoomDto room = gameRooms.get(roomId);
        if(room != null) {
            for(PlayerDto player : room.getPlayers()) {
                        if(player.getUserId().equals(submittedPlayer.getUserId())) {
                            if(player.getUsedTime() == null) {
                                player.setUsedTime(submittedPlayer.getUsedTime());
                                room.setCount(room.getCount() + 1);
                            }
                            break;
                        }
            }

            if(room.getCount() == room.getHeadCount()) {
                room.getPlayers().sort(Comparator.comparingInt(PlayerDto::getUsedTime));
                room.setCount(0);
            }
        }
    }

    public PlayerDto startGame(String roomId) {
        GameRoomDto room = gameRooms.get(roomId);
        if (room == null) {
            log.info("방이 존재하지 않습니다: " + roomId);
            throw new GameRoomException("방이 존재하지 않습니다: " + roomId, HttpStatus.NOT_FOUND);
        }

        int currentTurn = room.getCurrentTurn() % room.getHeadCount();
        room.setCurrentTurn(currentTurn);

        PlayerDto speaker = room.getPlayers().get(currentTurn);
        speaker.setStartTime(LocalDateTime.now());

        for (int i = 0; i < room.getPlayers().size(); i++) {
            PlayerDto player = room.getPlayers().get(i);
            player.setSpeaker(i == currentTurn);
        }

        log.info("현재 발화자: " + speaker.getName() + ", 시작 시간: " + speaker.getStartTime());

        return speaker;
    }

    public VoteResultDto vote(String roomId, Long playerId, boolean isAgreed) {
        GameRoomDto room = gameRooms.get(roomId);
        if(room != null) {
            PlayerDto currentPlayer = room.getPlayers().stream()
                    .filter(player -> player.getUserId().equals(playerId))
                    .findFirst()
                    .orElse(null);

            if (currentPlayer == null) {
                throw new IllegalArgumentException("플레이어를 찾지 못했습니다.");
            }

            if(isAgreed) {
                room.setAgreeCount(room.getAgreeCount() + 1);
            } else {
                room.getDisagreePlayers().add(currentPlayer.getName());
            }

            room.setCount(room.getCount() + 1);

            if(room.getHeadCount() - 1 == room.getCount()) {
                Boolean isEarned = null;
                if(room.getAgreeCount() >= room.getHeadCount() - 2) {
                    PlayerDto speaker = room.getPlayers().get(room.getCurrentTurn());
                    speaker.setPuzzlePiece(speaker.getPuzzlePiece() + 1);
                    room.setTotalPuzzlePiece(room.getTotalPuzzlePiece() + 1);
                    isEarned = true;
                } else {
                    isEarned = false;
                }

                boolean isGameEnd = false;
                if (room.getHeadCount() == 3) {
                    isGameEnd = room.getPlayers().stream().allMatch(player -> player.getPuzzlePiece() >= 10);
                } else if (room.getHeadCount() == 4) {
                    isGameEnd = room.getTotalPuzzlePiece() >= 13;
                }
                //fixme
                //  count, agreeCount 초기화 해야됨
                return VoteResultDto.builder()
                        .success(isGameEnd)
                        .disagreePlayers(room.getDisagreePlayers())
                        .agreeCount(room.getAgreeCount())
                        .disagreeCount(room.getCount() - room.getAgreeCount())
                        .isEarned(isEarned)
                        .build();
            }
        }

        return VoteResultDto.builder()
                .success(false)
                .disagreePlayers(room.getDisagreePlayers())
                .agreeCount(room.getAgreeCount())
                .disagreeCount(room.getCount() - room.getAgreeCount())
                .isEarned(null)
                .build();
    }

    public List<PlayerDto> calculateRankings(String roomId) {
        GameRoomDto room = gameRooms.get(roomId);

        if (room == null || room.getPlayers() == null || room.getPlayers().isEmpty()) {
            return Collections.emptyList();
        }

        room.getPlayers().sort(Comparator.comparingInt(PlayerDto::getPuzzlePiece).reversed());

        return room.getPlayers();
    }
}
