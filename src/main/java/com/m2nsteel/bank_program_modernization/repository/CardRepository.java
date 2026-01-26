package com.m2nsteel.bank_program_modernization.repository;

import com.m2nsteel.bank_program_modernization.domain.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {
    @Query(value = "SELECT nextval('card_num_seq')", nativeQuery = true)
    Long getNextCardSequence();

    Optional<Card> findByCardNum(String cardNum);
}
