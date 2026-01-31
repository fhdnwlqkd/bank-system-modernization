package com.m2nsteel.bank_program_modernization.repository.transaction;

import com.m2nsteel.bank_program_modernization.domain.constant.TransactionType;

import java.time.LocalDate;

public record TransactionQueryCriteria(
        String accountExternalId,
        LocalDate startDate,
        LocalDate endDate,
        TransactionType type,
        Long minAmount,
        Long maxAmount,
        Long lastId
) {
}
