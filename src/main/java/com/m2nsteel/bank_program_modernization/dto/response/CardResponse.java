package com.m2nsteel.bank_program_modernization.dto.response;

import java.time.LocalDateTime;

public record CardResponse(
        Long id,
        String maskedCardNumber, // 예: 9410-****-****-1234
        String accountNumber,    // 연결된 계좌 번호
        String cardType,
        String status,           // ACTIVE, BLOCKED, EXPIRED
        LocalDateTime expiredAt  // 유효 기간
) {
}
