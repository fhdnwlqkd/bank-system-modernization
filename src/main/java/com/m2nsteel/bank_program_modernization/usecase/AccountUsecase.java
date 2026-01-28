package com.m2nsteel.bank_program_modernization.usecase;

import org.jspecify.annotations.NullMarked;

import java.time.LocalDateTime;

@NullMarked
public class AccountUsecase {

    public record AccountCreateCommand(
            String memberExternalId, // 회원 식별자
            String branchCode,       // 지점 코드
            String accountPassword,
            String idempotencyKey    // 멱등성 키
    ) {}

    public record AccountResult(
            String externalId,       // 계좌 고유 식별자
            String accountNumber,
            Long balance,
            String status,
            LocalDateTime createdAt
    ) {}
}
