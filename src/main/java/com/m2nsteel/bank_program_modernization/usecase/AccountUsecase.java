package com.m2nsteel.bank_program_modernization.usecase;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

@NullMarked
public class AccountUsecase {

    public record AccountCreateCommand(
            String memberExternalId, // 회원 식별자
            String accountPassword
    ) {}

    public record AccountChangePasswordCommand(
            String password,
            String newPassword
    ) {}

    public record AccountResult(
            String externalId,       // 계좌 고유 식별자
            String accountNumber,
            Long balance,
            String status,
            LocalDateTime createdAt
    ) {}
}
