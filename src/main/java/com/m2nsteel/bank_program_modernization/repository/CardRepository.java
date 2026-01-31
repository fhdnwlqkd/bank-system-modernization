package com.m2nsteel.bank_program_modernization.repository;

import com.m2nsteel.bank_program_modernization.domain.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {
    @Query(value = "SELECT nextval('card_num_seq')", nativeQuery = true)
    Long getNextCardSequence();
    Optional<Card> findByExternalId(String externalId);
    boolean existsByExternalId(String externalId);

    @Query("select c from Card c " +
            "join fetch c.account a " + // 계좌 정보 미리 로딩
            "where a.member.externalId = :memberExternalId")
    List<Card> findAllByMemberExternalId(@Param("memberExternalId") String memberExternalId);
}
