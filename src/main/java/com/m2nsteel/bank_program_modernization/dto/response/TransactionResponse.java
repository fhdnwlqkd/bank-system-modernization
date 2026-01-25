package com.m2nsteel.bank_program_modernization.dto.response;

import java.time.LocalDateTime;

public record TransactionResponse (
    Long transactionId,      // 거래 고유 번호
    String accountNumber,    // 관련 계좌 번호
    String type,             // DEPOSIT, WITHDRAW, TRANSFER
    Long amount,             // 거래 금액
    Long balanceAfter,       // 거래 후 잔액 (매우 중요!)
    String description,      // 거래 메모
    LocalDateTime createdAt  // 거래 일시
) {
}
