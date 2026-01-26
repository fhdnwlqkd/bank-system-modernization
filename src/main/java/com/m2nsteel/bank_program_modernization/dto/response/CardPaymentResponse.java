package com.m2nsteel.bank_program_modernization.dto.response;

import java.time.LocalDateTime;

public record CardPaymentResponse(
        Long transactionId,
        String maskedCardNumber,
        Long amount,
        Long balanceAfter,   // 결제 후 남은 계좌 잔액
        String businessRegistrationNumber,
        String status,       // SUCCESS, FAIL
        LocalDateTime occurredAt
) {
}
