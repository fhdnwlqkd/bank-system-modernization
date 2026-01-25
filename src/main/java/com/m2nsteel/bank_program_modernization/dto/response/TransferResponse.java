package com.m2nsteel.bank_program_modernization.dto.response;

import java.time.LocalDateTime;

public record TransferResponse(
        Long transactionId,
        String fromAccountNumber,
        String toAccountNumber,
        Long amount,
        Long balanceAfter,
        String status,
        LocalDateTime occurredAt
) {
}
