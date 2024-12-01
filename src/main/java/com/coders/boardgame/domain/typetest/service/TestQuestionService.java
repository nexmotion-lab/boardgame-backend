package com.coders.boardgame.domain.typetest.service;

import com.coders.boardgame.domain.typetest.dto.TestOptionDto;
import com.coders.boardgame.domain.typetest.dto.TestQuestionDto;
import com.coders.boardgame.domain.typetest.entity.TestQuestion;
import com.coders.boardgame.domain.typetest.repository.TestQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TestQuestionService {

    private final TestQuestionRepository testQuestionRepository;

    @Transactional(readOnly = true)
    public List<TestQuestionDto> getQuestionsByServiceType(Byte serviceTypeId){
        List<TestQuestion> questions = testQuestionRepository.findQuestionByServiceTypeWithOptions(serviceTypeId);

        return questions.stream()
                .map(question -> new TestQuestionDto(
                        question.getId(),
                        question.getContent(),
                        question.getTestOptions().stream()
                                .map(option -> new TestOptionDto(
                                        option.getId(),
                                        option.getContent(),
                                        option.getScore()
                                ))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

}
