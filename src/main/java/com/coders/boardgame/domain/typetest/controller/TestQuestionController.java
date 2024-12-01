package com.coders.boardgame.domain.typetest.controller;


import com.coders.boardgame.domain.typetest.dto.TestQuestionDto;
import com.coders.boardgame.domain.typetest.service.TestQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test-questions")
public class TestQuestionController {

    private final TestQuestionService testQuestionService;

    @GetMapping("/service-type/{serviceTypeId}")
    public ResponseEntity<List<TestQuestionDto>> getQuestionsByServiceType(@PathVariable Byte serviceTypeId){
        List<TestQuestionDto> questions = testQuestionService.getQuestionsByServiceType(serviceTypeId);
        return ResponseEntity.ok(questions);
    }

}