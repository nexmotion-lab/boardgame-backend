package com.coders.boardgame.domain.game.service;

import com.coders.boardgame.domain.game.dto.GameRoomDto;
import com.coders.boardgame.domain.game.dto.GameStateDto;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;


@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private final GameRoomService gameRoomService;
    private final GameSseService gameSseService;

    private final Map<String, Map<Long, String>> votes = new ConcurrentHashMap<>();

    /**
     * 게임 시작 시 게임정보를 SSE로 방에 있는 모든 클라이언트한테 전달
     *
     * @param roomId 방 id
     * @param hostId 호스트 id
     */
    public void startGame(String roomId, Long hostId) {
        GameRoomDto room = gameRoomService.getRoom(roomId);

        // 호스트 확인
        if (!room.getHostId().equals(hostId)) {
            throw new GameRoomException("게임을 시작할 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }

        // 참가자 수 확인
        if (room.getCurrentPlayers().get() < room.getTotalPlayers()) {
            throw new GameRoomException("모든 참가자가 준비되지 않습니다.", HttpStatus.CONFLICT);
        }

        // 모든 조건이 충족되었으므로 게임 시작 상태 변경
        if (!room.getIsGameStarted().compareAndSet(false, true)) {
            throw new GameRoomException("이미 게임이 시작되었습니다.", HttpStatus.CONFLICT);
        }

        // 게임 상태 초기화
        room.setCurrentTurn(1);
        room.setCurrentPuzzlePieces(0);
        room.setCurrentRound(1);

        // GameStateDto 생성
        GameStateDto gameState = GameStateDto.builder()
                .roomId(roomId)
                .roomName(room.getRoomName())
                .totalPlayers(room.getTotalPlayers())
                .totalPuzzlePieces(room.getTotalPuzzlePieces())
                .currentPuzzlePieces(room.getCurrentPuzzlePieces())
                .currentTurn(room.getCurrentTurn())
                .currentRound(room.getCurrentRound())
                .players(new ArrayList<>(room.getPlayers().values()))
                .build();

        // 게임 시작 알림
        gameSseService.sendRoomEvent(roomId, "game-started", gameState);
        log.info("게임 시작됨: roomId={}, hostId={}", roomId, hostId);
    }

    /**
     * 스마트폰 사용 시간을 설정하고, 모든 사용자가 입력 완료 시 순번 배치 및 SSE 브로드 캐스트
     *
     * @param roomId    방 ID
     * @param playerId  플레이어 ID
     * @param usageTime 스마트폰 사용 시간
     */
    public void setUsageTime(String roomId, Long playerId, int usageTime) {
        GameRoomDto room = gameRoomService.getRoom(roomId);

        // 플레이어가 방에 존재하는 지 확인
        PlayerDto player = room.getPlayers().computeIfPresent(playerId, (id, p) -> {
            p.setUsageTime(usageTime);
            return p;
        });

        if (player == null) {
            throw new GameRoomException("플레이어가 방에 존재하지 않습니다.", HttpStatus.NOT_FOUND);
        }

        // 모든 플레이어가 사용 시간을 입력했는지 확인
        boolean allPlayersSet = room.getPlayers().values().stream()
                .allMatch(p -> p.getUsageTime() > 0);

        if (allPlayersSet) {
            // 순번 매기기: 사용시간 -> 설문 점수 -> 랜덤 순서
            List<PlayerDto> sortedPlayers = new ArrayList<>(room.getPlayers().values());
            sortedPlayers.sort(Comparator.comparingInt(PlayerDto::getUsageTime)
                    .thenComparingInt(PlayerDto::getSurveyScore).reversed()
                    .thenComparing(p -> ThreadLocalRandom.current().nextInt()));

            // 순번 할당
            int[] index = {1};
            sortedPlayers.forEach(p -> {
                room.getPlayers().computeIfPresent(p.getPlayerId(), (id, p1) -> {
                    p1.setSequenceNumber(index[0]++);
                    return p1;
                });
            });

            // 순번 배치 완료 이벤트 전송
            Map<Long, Integer> playerOrder = new HashMap<>();
            sortedPlayers.forEach(p -> playerOrder.put(p.getPlayerId(), p.getSequenceNumber()));
            gameSseService.sendRoomEvent(roomId, "player-order-assigned", playerOrder);

            log.info("순번 배치 완료: roomId= {}, 순번 = {}", roomId, playerId);
        } else {
            // 플레이어 이름과 함께 로그 출력
            String playerName = player.getPlayerInfo().getName();
            log.info("플레이어 '{}'가 사용 시간을 설정했습니다: roomId={}, usageTime={}분", playerName, roomId, usageTime);
        }
    }

    /**
     * 라운드 시작
     *
     * @param roomId      방 ID
     * @param roundNumber 라운드 번호
     * @param userId      요청한 사용자 ID
     */
    public void startRound(String roomId, int roundNumber, Long userId) {
        GameRoomDto room = gameRoomService.getRoom(roomId);

        // 호스트 확인
        if (!room.getHostId().equals(userId)) {
            throw new GameRoomException("라운드를 시작할 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }

        // 게임 시작 여부 확인
        if (!room.getIsGameStarted().get()) {
            throw new GameRoomException("게임이 아직 시작되지 않았습니다.", HttpStatus.CONFLICT);
        }

        // 라운드 상태 업데이트
        room.setCurrentRound(roundNumber);

        // 첫 번쨰 순번인 플레이어 찾기
        PlayerDto speaker = room.getPlayers().values().stream()
                .filter(p -> p.getSequenceNumber() == 1)
                .findFirst()
                .orElseThrow(() -> new GameRoomException("순번이 지정된 플레이어가 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR));

        // speaker 미리 저장
        Long speakerId = speaker.getPlayerId();

        // 모든 사용자에게 적절한 이벤트 전송
        room.getPlayers().forEach((playerId, player) -> {
            boolean isSpeaking = playerId.equals(speakerId);
            player.setSpeaking(isSpeaking);

            Map<String, Object> eventData = Map.of(
                    "round", roundNumber,
                    "role", isSpeaking ? "speaking" : "listening",
                    "speakerId", speakerId
            );

            gameSseService.sendToSpecificPlayer(roomId, playerId, "round-" + roundNumber + "-started", eventData);
        });
        log.info("라운드 {}가 시작되었습니다: roomId={}, speakerId={}", roundNumber, roomId, speakerId);
    }

    /**
     * 방에 카드 타입을 부여하고, 모든 카드가 부여되었으면 타이머를 시작
     * 이 메서드는 특정 방(roomId)에 지정된 카드 타입(cardType)을 부여
     * "picture" 또는 "text" 타입의 카드가 부여될 수 있으며, 모든 카드가 부여되었는지 확인 후
     * 타이머 시작 이벤트를 전송. 이때, 클라이언트마다 응답시간 반영해서 타이머 조정 필요
     *
     * @param roomId   카드가 부여될 방의 ID
     * @param maxId    생성 가능한 카드 ID의 최대값
     * @param cardType 부여할 카드의 타입 (예: "picture", "text")
     * @throws GameRoomException 잘못된 카드 타입이 입력되었을 때 발생
     */
    public void assignCard(String roomId, int maxId, String cardType) {

        GameRoomDto room = gameRoomService.getRoom(roomId);


        // 카드 상태 업데이트
        if ("picture".equals(cardType)) {
            room.setPictureCardAssigned(true);
        } else if ("text".equals(cardType)) {
            room.setTextCardAssigned(true);
        } else {
            throw new GameRoomException("잘못된 카드 타입입니다.", HttpStatus.FORBIDDEN);
        }

        int assignedCardId = ThreadLocalRandom.current().nextInt(1, maxId + 1);

        // 이벤트 데이터 생성
        Map<String, Object> eventData = Map.of(
                "cardType", cardType,
                "cardId", assignedCardId
        );

        gameSseService.sendRoomEvent(roomId, "card-assigned", eventData);
        log.info("{}에 {}타입의 카드가 부여되었습니다.", roomId, cardType);

        // 모든 카드가 부여되었는지 확인
        if (room.isPictureCardAssigned() && room.isTextCardAssigned()) {
            // 타이머 시작 이벤트 데이터 생성
            Map<String, Object> timerEventData = Map.of(
                    "startTime", System.currentTimeMillis()
            );
            gameSseService.sendRoomEvent(roomId, "timer-start", timerEventData);
            log.info("{}안에 카드가 모두 부여되어서 타이머가 시작합니다.", roomId);
        }
    }

    /**
     * 시간 연장
     *
     * @param roomId         방 id
     * @param additionalTime 추가할 시간
     */
    public void extendTime(String roomId, int additionalTime) {
        GameRoomDto room = gameRoomService.getRoom(roomId);
        Map<String, Object> eventData = Map.of(
                "additionalTime", additionalTime
        );
        gameSseService.sendRoomEvent(room.getRoomId(), "time-extended", eventData);
    }

    /**
     * 말하기 종료
     *
     * @param roomId 방 id
     */
    public void endSpeaking(String roomId) {
        GameRoomDto room = gameRoomService.getRoom(roomId);
        gameSseService.sendRoomEvent(room.getRoomId(), "speaking-end", "말하기가 종료되었습니다.");
    }

    /**
     * 플레이어의 투표를 기록하고, 모든 플레이어의 투표가 완료되면 결과를 처리
     * 이 메서드는 특정 방(roomId)에 속한 플레이어(playerId)의 투표(vote)를 기록
     * 방에 속한 모든 플레이어(총 인원 - 1)가 투표를 완료하면 `processVoteResults`를 호출
     * 이후, 투표 결과를 처리합니다.
     *
     * @param roomId  방의 ID
     * @param playerId  투표를 한 플레이어의 ID
     * @param vote  플레이어가 선택한 투표 결과 ("agree" 또는 "disagree")
     */
    public void castVote(String roomId, Long playerId, String vote) {
        GameRoomDto room = gameRoomService.getRoom(roomId);

        votes.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(playerId, vote);

        if(votes.get(roomId).size() == room.getTotalPlayers() -1){
            processVoteResults(room);
        }
    }

    /**
     * 투표 결과를 처리하고, 퍼즐 조각 획득 여부를 결정.
     * 투표 결과를 기반으로 찬성(agree) 및 반대(disagree)의 개수를 계산하고,
     * 퍼즐 조각을 획득했는지 여부를 판단
     * 투표 결과에 따라 퍼즐 조각 획득(success), 재투표(re-vote), 혹은 실패(failure) 이벤트를 발생
     *
     * @param room  투표 결과를 처리할 방 정보 (GameRoomDto)
     */
    private void processVoteResults(GameRoomDto room) {
        Map<Long, String> roomVotes = votes.get(room.getRoomId());

        int totalPlayers = room.getTotalPlayers();
        int agreeCount = (int) roomVotes.values().stream().filter(v -> v.equals("agree")).count();
        int disagreeCount = (int) roomVotes.values().stream().filter(v -> v.equals("disagree")).count();


        boolean puzzleAcquired;
        if (totalPlayers == 3){
            puzzleAcquired = agreeCount >= 1; // 3명일 경우, 찬성 1명이면 획득
        } else {
            int majority = (int) Math.ceil(totalPlayers / 2.0); // 과반수 기준
            puzzleAcquired = agreeCount >= majority;
        }

        if(puzzleAcquired){
            // 퍼즐 조각 획득 처리
            PlayerDto currentSpeaker = getCurrentSpeaker(room);
            currentSpeaker.setCollectedPuzzlePieces(currentSpeaker.getCollectedPuzzlePieces()+1);
            room.setCurrentPuzzlePieces(room.getCurrentPuzzlePieces() + 1);

            Map<String, Object> eventData = Map.of(
                    "result", "success",
                    "agreeCount", agreeCount,
                    "disagreeCount", disagreeCount
            );

            gameSseService.sendRoomEvent(room.getRoomId(), "vote-result-success", eventData);
        } else {
            List<String> disagreePlayers = roomVotes.entrySet().stream()
                    .filter(entry -> entry.getValue().equals("disagree"))
                    .map(entry -> room.getPlayers().get(entry.getKey()).getPlayerInfo().getName())
                    .toList();

            if(!room.isHasReVoted()){
                Map<String, Object> eventData = Map.of(
                        "result", "re-vote",
                        "agreeCount", agreeCount,
                        "disagreeCount", disagreeCount,
                        "disagreePlayers", disagreePlayers
                );
                gameSseService.sendRoomEvent(room.getRoomId(), "vote-result-re-vote", eventData);
                room.setHasReVoted(true);
            }
            else {
                Map<String, Object> eventData = Map.of(
                        "result", "failure",
                        "agreeCount", agreeCount,
                        "disagreeCount", disagreeCount
                );
                gameSseService.sendRoomEvent(room.getRoomId(), "vote-result-failure", eventData);
            }
        }
    }

    /**
     * 현재 발화 중인 플레이어를 반환
     * 방의 플레이어 목록에서 `isSpeaking` 상태가 true인 플레이어를 검색하여 반환
     * 만약 현재 발화자가 없을 경우 예외를 발생
     *
     * @param room  발화자를 검색할 방 정보 (GameRoomDto)
     * @return 현재 발화 중인 플레이어 (PlayerDto)
     * @throws IllegalStateException 현재 발화자가 없을 경우 발생
     */
    private PlayerDto getCurrentSpeaker(GameRoomDto room) {
        return room.getPlayers().values().stream()
                .filter(PlayerDto::isSpeaking)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("현재 발화자를 찾을 수 없습니다."));
    }



    /**
     * 게임 상태 초기화
     *
     * @param roomId 방 id
     */
    private void resetGame(String roomId) {
        GameRoomDto room = gameRoomService.getRoom(roomId);

        // 게임 상태 초기화
        room.setIsGameStarted(new AtomicBoolean(false));
        room.setCurrentRound(0);
        room.setCurrentTurn(0);
        room.setCurrentPuzzlePieces(0);

        // 플레이어 상태 초기화
        room.getPlayers().values().forEach(player -> {
            player.setSequenceNumber(0);
            player.setUsageTime(0);
            player.setSpeaking(false);
        });

        // 초기화 완료 이벤트 전송
        gameSseService.sendRoomEvent(roomId, "game-reset", "게임이 초기화되었습니다. 호스트가 게임을 다시 시작할 수 있습니다");
        log.info("게임이 초기화되었습니다: roomId={}", roomId);
    }
}
