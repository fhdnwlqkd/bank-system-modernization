package com.m2nsteel.bank_program_modernization.repository.transaction;

import com.m2nsteel.bank_program_modernization.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, TransactionRepositoryCustom {
    Boolean existsByIdempotencyKey(String idempotencyKey);
}
