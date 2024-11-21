package com.coders.boardgame.controller;

import com.coders.boardgame.dto.SchoolInfoDto;
import com.coders.boardgame.service.SchoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/schools")
public class SchoolController {


    @Autowired
    private SchoolService schoolService;

    @GetMapping("/search")
    public ResponseEntity<List<SchoolInfoDto>> searchSchools(@RequestParam("query") String query){
        List<SchoolInfoDto> results = schoolService.searchSchools(query);
        return ResponseEntity.ok(results);
    }
}
