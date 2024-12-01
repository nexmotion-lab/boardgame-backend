package com.coders.boardgame.domain.typetest.service;

import com.coders.boardgame.domain.typetest.dto.TestResultRequest;
import com.coders.boardgame.domain.typetest.dto.TestResultResponse;
import com.coders.boardgame.domain.typetest.entity.*;
import com.coders.boardgame.domain.typetest.repository.SuhatTypeRepository;
import com.coders.boardgame.domain.typetest.repository.TestOptionRepository;
import com.coders.boardgame.domain.typetest.repository.TestResultOptionRepository;
import com.coders.boardgame.domain.typetest.repository.TestResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class TestResultService {

    private final TestResultRepository testResultRepository;
    private final SuhatTypeRepository suhatTypeRepository;
    private final TestOptionRepository testOptionRepository;
    private final TestResultOptionRepository testResultOptionRepository;


    @Transactional
    public TestResultResponse saveTestResult(TestResultRequest requestDto, Long userId) {

        // 1. suhatType 조회
        SuhatType suhatType = suhatTypeRepository.findByServiceTypeAndScoreRange(
                requestDto.getServiceTypeId(),
                requestDto.getTotalScore()
        ).orElseThrow(() -> new IllegalArgumentException("점수 범위에 해당하는 suHatType을 찾을 수 없습니다."));

        // 2. TestResult 저장
        TestResult testResult = TestResult.builder()
                .userId(userId)
                .suhatType(suhatType)
                .testDate(LocalDateTime.now())
                .build();
        TestResult savedTestResult = testResultRepository.save(testResult);

        // 3. TestResultOption 저장
        if (requestDto.getSelectedOptionIds() != null) {
            saveSelectedOptions(requestDto.getSelectedOptionIds(), savedTestResult);
        }

        // 4. testResultResponse 반환
        return new TestResultResponse(
                suhatType.getId(),
                suhatType.getName(),
                suhatType.getDescription(),
                suhatType.getSuggestion()
                );
    }

    private void saveSelectedOptions(List<Integer> selectedOptionIds, TestResult savedTestResult) {
        // selectedOptionIds에 해당하는 TestOption을 조회
        List<TestOption> selectedOptions = testOptionRepository.findAllById(selectedOptionIds);

        // TestResultOption 객체 생성
        List<TestResultOption> resultOptions = selectedOptions.stream()
                .map(option -> new TestResultOption(
                        new TestResultOptionId(savedTestResult.getId(), option.getId()),
                        savedTestResult,
                        option // 유효한 testOption을 설정
                ))
                .toList();

        // TestResultOption 저장
        testResultOptionRepository.saveAll(resultOptions);
    }
}
