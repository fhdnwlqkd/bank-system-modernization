package com.m2nsteel.bank_program_modernization.repository;

import com.m2nsteel.bank_program_modernization.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByExternalId(String externalId);
}
