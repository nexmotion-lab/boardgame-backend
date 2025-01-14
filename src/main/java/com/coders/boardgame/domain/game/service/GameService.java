package com.coders.boardgame.domain.game.service;

import com.coders.boardgame.domain.game.dto.GameRoomDto;
import com.coders.boardgame.domain.game.dto.GameStateDto;
import com.coders.boardgame.domain.game.dto.PlayerDto;
import com.coders.boardgame.exception.GameRoomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;


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
        if (room.getRoomStatus() != GameRoomDto.RoomStatus.WAITING) {
            throw new GameRoomException("이미 게임이 시작되었습니다.", HttpStatus.CONFLICT);
        }

        // 방 상태르 IN_GAME으로 변경
        room.setRoomStatus(GameRoomDto.RoomStatus.IN_GAME);

        // 게임 상태 초기화
        room.setCurrentTurn(1);
        room.setCurrentPuzzlePieces(0);
        room.setCurrentRound(1);

        // 게임 시작 알림
        gameSseService.sendRoomEvent(roomId, "game-started", roomId);
        log.info("게임 시작됨: roomId={}, hostId={}", roomId, hostId);
    }

    public GameStateDto getGameState(String roomId, Long playerId) {
        GameRoomDto room = gameRoomService.getRoom(roomId);

        if (!room.getPlayers().containsKey(playerId)) {
            throw new GameRoomException("해당 방에 참여하고 있지 않습니다.", HttpStatus.FORBIDDEN);
        }

        // GameStateDto 생성
        return GameStateDto.builder()
                .roomId(roomId)
                .roomName(room.getRoomName())
                .hostId(room.getHostId())
                .totalPlayers(room.getTotalPlayers())
                .totalPuzzlePieces(room.getTotalPuzzlePieces())
                .currentPuzzlePieces(room.getCurrentPuzzlePieces())
                .currentTurn(room.getCurrentTurn())
                .currentRound(room.getCurrentRound())
                .players(new ArrayList<>(room.getPlayers().values()))
                .build();

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
        if (room.getRoomStatus() != GameRoomDto.RoomStatus.IN_GAME) {
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

        // 발화자 설정 (서버 측에서 상태 관리하기
        speaker.setSpeaking(true);

        Map<String, Object> eventData = Map.of(
                "round", roundNumber,
                "speakerId", speaker.getPlayerId()
        );

        gameSseService.sendRoomEvent(roomId, "round-" + roundNumber + "-started", eventData);

        log.info("라운드 {}가 시작되었습니다: roomId={}, speakerId={}", roundNumber, roomId, speaker.getPlayerId());
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
     * 이후, 투표 결과를 처리
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
     * 게임 상태 초기화
     * @param roomId 방 id
     */
    public void resetGame(String roomId) {
        GameRoomDto room = gameRoomService.getRoom(roomId);

        // 게임 상태 초기화
        room.setCurrentRound(0);
        room.setCurrentTurn(0);
        room.setCurrentPuzzlePieces(0);
        room.setHasReVoted(false);

        // 플레이어 상태 초기화
        room.getPlayers().values().forEach(this::resetPlayerState);

        // 투표 데이터 초기화
        votes.remove(roomId);

        // 방 상태를 WAITING으로 변경
        room.setRoomStatus(GameRoomDto.RoomStatus.WAITING);

        // 초기화 완료 이벤트 전송
        gameSseService.sendRoomEvent(roomId, "game-reset", "게임이 초기화되어 대기방으로 이동");

        gameRoomService.reassignHostIfNeed(roomId);

        log.info("게임이 초기화되었습니다: roomId={}", roomId);
    }

    /**
     * 투표 결과를 처리하고, 퍼즐 조각 획득 여부를 결정.
     * 투표 결과를 기반으로 찬성(agree) 및 반대(disagree)의 개수를 계산하고,
     * 퍼즐 조각을 획득했는지 여부를 판단
     * 투표 결과에 따라 퍼즐 조각 획득(success), 재투표(re-vote), 혹은 실패(failure) 이벤트를 발생 시키고
     * 퍼즐 조각이 남아있을 시 다음 턴으로 이동, 만약 퍼즐 조각을 다 모은 경우 게임을 완료하고 순위 결정
     *
     * @param room  투표 결과를 처리할 방 정보 (GameRoomDto)
     */
    private void processVoteResults(GameRoomDto room) {
        Map<Long, String> roomVotes = votes.get(room.getRoomId());

        int totalPlayers = room.getTotalPlayers();
        int agreeCount = (int) roomVotes.values().stream().filter(v -> v.equals("agree")).count();
        int disagreeCount = (int) roomVotes.values().stream().filter(v -> v.equals("disagree")).count();


        boolean puzzleAcquired = totalPlayers == 3
                ? agreeCount >= 1
                : agreeCount >= (int) Math.ceil(totalPlayers / 2.0);

        Map<String, Object> eventData = puzzleAcquired
                ? handlePuzzleAcquired(room, agreeCount, disagreeCount)
                : handlePuzzleFailed(room, agreeCount, disagreeCount, roomVotes);

        // result를 확인해서 분기
        String result = (String) eventData.get("result");

        if ("puzzle-acquired-next-turn".equals(result) || "puzzle-failed-next-turn".equals(result)){
           moveToNextTurn(room);
           sendNextTurnEventToEachPlayer(room, eventData);
        } else {
            gameSseService.sendRoomEvent(room.getRoomId(), "vote-result", eventData);
        }
        log.info("투표 결과 처리 완료: roomId={}, eventData={}", room.getRoomId(), eventData);
    }

    /**
     * 퍼즐 획득 성공 처리
     * @param room 방 정보
     * @param agreeCount 찬성표 개수
     * @param disagreeCount 반대표 개수
     * @return sse 이벤트 데이터
     */
    private Map<String, Object> handlePuzzleAcquired(GameRoomDto room, int agreeCount, int disagreeCount) {
        PlayerDto currentSpeaker = getCurrentSpeaker(room);
        currentSpeaker.setCollectedPuzzlePieces(currentSpeaker.getCollectedPuzzlePieces()+1);
        room.setCurrentPuzzlePieces(room.getCurrentPuzzlePieces() + 1);

        // 퍼즐 획득 방식 추가(재투표 여부)
        boolean hasReVoted = room.isHasReVoted();
        room.setHasReVoted(false);

        if(room.getCurrentPuzzlePieces() >= room.getTotalPuzzlePieces()){
            // 게임 완료 처리
            List<Map<String, Object>> rankingData = completeGameAndRankPlayers(room);
            // 방 상태 초기화
            resetRoomAfterCompletion(room);
            return Map.of(
                    "result", "game-completed",
                    "agreeCount", agreeCount,
                    "disagreeCount", disagreeCount,
                    "rankingData", rankingData,
                    "hasReVoted", hasReVoted // 재투표 여부

            );
        } else {
            return Map.of(
                    "result", "puzzle-acquired-next-turn",
                    "agreeCount", agreeCount,
                    "disagreeCount", disagreeCount,
                     "hasReVoted", hasReVoted // 재투표 여부
            );
        }
    }


    /**
     * 퍼즐 획득 실패 처리
     * @param room 방정보
     * @param agreeCount 찬성표 개수
     * @param disagreeCount 반대표 개수
     * @param roomVotes 투표 결과
     * @return sse 이벤트 데이터
     */
    private Map<String, Object> handlePuzzleFailed(GameRoomDto room, int agreeCount, int disagreeCount, Map<Long, String> roomVotes) {
        List<String> disagreePlayers = roomVotes.entrySet().stream()
                .filter(entry -> entry.getValue().equals("disagree"))
                .map(entry -> room.getPlayers().get(entry.getKey()).getPlayerInfo().getName())
                .toList();

        if(!room.isHasReVoted()){
            room.setHasReVoted(true);
            return Map.of(
                    "result", "re-vote",
                    "agreeCount", agreeCount,
                    "disagreeCount", disagreeCount,
                    "disagreePlayers", disagreePlayers
            );
        } else {
            // 퍼즐 획득 실패 및 다음 턴으로 이동
            room.setHasReVoted(false);
            return Map.of(
                    "result", "puzzle-failed-next-turn",
                    "agreeCount", agreeCount,
                    "disagreeCount", disagreeCount
            );
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
     * 다음 턴으로 이동. 새로운 발화자를 설정하고 및 방 현재 퍼즐 갯수 증가
     * @param room 다음 턴으로 진행할 방 정보 (GameRoomDto)
     * @return 다음 발화해야하는 플레이어 (PlayerDto)
     */
    private void moveToNextTurn(GameRoomDto room){
        // 현재 턴 증가 (순환)
        int nextTurn = (room.getCurrentTurn() % room.getTotalPlayers()) + 1;
        room.setCurrentTurn(nextTurn);

        // 기존 발화자의 상태를 초기화
        room.getPlayers().values().stream()
                .filter(PlayerDto::isSpeaking)
                .findFirst()
                .ifPresent(player -> player.setSpeaking(false));

        // 새로운 발화자 설정 및 반환
        PlayerDto nextSpeaker = room.getPlayers().values().stream()
                .filter(player -> player.getSequenceNumber() == nextTurn)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("다음 발화자를찾을 수 없습니다."));

        room.setPictureCardAssigned(false);
        room.setTextCardAssigned(false);

        // 다음 발화자를 발화 상태로 설정
        nextSpeaker.setSpeaking(true);

        log.info("다음 턴으로 이동: roomId={}, nextSpeakerId={}, currentTurn={}",
                room.getRoomId(), nextSpeaker.getPlayerId(), nextTurn);

    }

    /**
     * 게임 완료 시 퍼즐 완성 및 순위 결졍
     * @param room 게임이 완료된 방 정보 (GameRoomDto)
     */
    private List<Map<String, Object>> completeGameAndRankPlayers(GameRoomDto room){
        // 플레이어 랭킹 계산
        List<PlayerDto> rankedPlayers = room.getPlayers().values().stream()
                .sorted(Comparator.comparingInt(PlayerDto::getCollectedPuzzlePieces).reversed()
                        .thenComparingInt(PlayerDto::getSequenceNumber))
                .toList();

        // 랭킹 결과 데이터 생성
        return rankedPlayers.stream()
                .map(player -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("playerId", player.getPlayerId());
                    map.put("playerName", player.getPlayerInfo().getName());
                    map.put("puzzlePieces", player.getCollectedPuzzlePieces());
                    return map;
                })
                .toList();
    }

    /**
     * 다음 턴에서 각 플레이어의 역할(speaker/listener)을 알리는 SSE 전송 메서드.
     * (puzzle-acquired-next-turn / puzzle-failed-next-turn 시점에 호출)
     *
     * @param room     다음 턴 정보를 보낼 게임 방
     * @param baseData 투표 결과 등 공통 정보 (result, agreeCount 등)
     */
    private void sendNextTurnEventToEachPlayer(GameRoomDto room, Map<String, Object> baseData){
        PlayerDto speaker = getCurrentSpeaker(room);
        Long speakerId = speaker.getPlayerId();

        // 모든 플레이어에게 role을 구분해서 전송
        room.getPlayers().forEach((playerId, player) ->{
            boolean isSpeaking = playerId.equals(speakerId);

            // eventData에 추가필드를 넣어 전송
            HashMap<String, Object> finalData = new HashMap<>(baseData);
            finalData.put("nextRole", isSpeaking ? "speaker" : "listener");
            finalData.put("nextSpeakerId", speakerId);

            gameSseService.sendToSpecificPlayer(room.getRoomId(), playerId, "vote-result", finalData);
        });
    }

    /**
     * 퍼즐 완료 후, 게임 관련 상태 초기화
     * @param room 초기화할 게임방
     */
    private void resetRoomAfterCompletion(GameRoomDto room) {
        // 게임 관련 필드 초기화
        room.setCurrentTurn(0); //
        room.setCurrentPuzzlePieces(0);
        room.setRoomStatus(GameRoomDto.RoomStatus.ENDED);
        room.setHasReVoted(false);

        // 플레이어 상태 초기화
        room.getPlayers().values().forEach(this::resetPlayerState);

        // 투표 데이터 초기화
        votes.remove(room.getRoomId());

        log.info("방 상태 초기화 완료: roomId={}", room.getRoomId());
    }

    /**
     * 게임 진행중 플레이어 상태 초기화
     * @param player
     */
    private void resetPlayerState(PlayerDto player) {
        player.setCollectedPuzzlePieces(0);
        player.setSequenceNumber(0);
        player.setSpeaking(false);
        player.setUsageTime(0); // 사용 시간 초기화
    }

}
