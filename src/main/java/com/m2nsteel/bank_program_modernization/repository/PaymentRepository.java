package com.m2nsteel.bank_program_modernization.repository;

import com.m2nsteel.bank_program_modernization.domain.Card;
import com.m2nsteel.bank_program_modernization.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByExternalId(String externalId);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    @Query("select p from Payment p " +
            "join fetch p.cardAccount a " +
            "where a.member.externalId = :memberExternalId")
    List<Payment> findAllByMemberExternalId(@Param("memberExternalId") String memberExternalId);
}
