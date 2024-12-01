package com.coders.boardgame.domain.typetest.repository;

import com.coders.boardgame.domain.typetest.entity.SuhatType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SuhatTypeRepository extends JpaRepository<SuhatType, Byte> {

    /**
     *
     * @param serviceTypeId 서비스 유형 Id
     * @param score score 점수
     * @return 적합한 suHatType
     */
    @Query("SELECT s " +
            "FROM SuhatType s " +
            "WHERE s.serviceType.id = :serviceTypeId " +
            "AND :score BETWEEN s.minScore AND s.maxScore")
    Optional<SuhatType> findByServiceTypeAndScoreRange(
            @Param("serviceTypeId") Byte serviceTypeId,
            @Param("score") Byte score
    );
}
