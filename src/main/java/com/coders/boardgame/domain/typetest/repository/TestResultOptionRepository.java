package com.coders.boardgame.domain.typetest.repository;

import com.coders.boardgame.domain.typetest.entity.TestResultOption;
import com.coders.boardgame.domain.typetest.entity.TestResultOptionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestResultOptionRepository extends JpaRepository<TestResultOption, TestResultOptionId> {
}
