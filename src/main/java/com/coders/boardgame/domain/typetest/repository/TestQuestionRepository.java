package com.coders.boardgame.domain.typetest.repository;

import com.coders.boardgame.domain.typetest.entity.TestQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestQuestionRepository extends JpaRepository<TestQuestion, Integer> {

    @Query("SELECT DISTINCT tq FROM TestQuestion tq " +
            "JOIN FETCH tq.testOptions " +
            "WHERE tq.serviceType.id = :serviceTypeId")
    List<TestQuestion> findQuestionByServiceTypeWithOptions(@Param("serviceTypeId") Byte serviceTypeId);

}
