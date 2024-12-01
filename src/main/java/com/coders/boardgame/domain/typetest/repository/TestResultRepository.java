package com.coders.boardgame.domain.typetest.repository;

import com.coders.boardgame.domain.typetest.entity.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface TestResultRepository extends JpaRepository<TestResult, Long> {
}
