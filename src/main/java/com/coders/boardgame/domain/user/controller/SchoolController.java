package com.coders.boardgame.domain.user.controller;

import com.coders.boardgame.domain.user.dto.SchoolInfoDto;
import com.coders.boardgame.domain.user.service.SchoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schools")
public class SchoolController {


    private final SchoolService schoolService;

    @GetMapping
    public ResponseEntity<List<SchoolInfoDto>> searchSchools(@RequestParam("query") String query){
        List<SchoolInfoDto> results = schoolService.searchSchools(query);
        return ResponseEntity.ok(results);
    }
}
