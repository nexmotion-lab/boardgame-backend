package com.coders.boardgame.domain.typetest.repository;

import com.coders.boardgame.domain.typetest.entity.TestOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface TestOptionRepository extends JpaRepository<TestOption, Integer> {

    @Query("SELECT t.id FROM TestOption t WHERE t.id IN :ids")
    List<Integer> findOptionIdsByIds(@Param("ids") List<Integer> ids);
}
