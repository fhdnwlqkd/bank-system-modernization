package com.m2nsteel.bank_program_modernization.usecase;

import org.jspecify.annotations.NullMarked;

import java.time.LocalDate;
import java.time.LocalDateTime;

@NullMarked
public class TransactionUsecase {

    // --- 입력(Command) ---
    public record DepositCommand(String accountNumber, Long amount, String idempotencyKey) {}

    public record WithdrawCommand(String accountNumber, Long amount, String accountPassword, String idempotencyKey) {}

    public record TransferCommand(String fromAccountNumber, String toAccountNumber, Long amount, String accountPassword, String idempotencyKey) {}

    public record SearchQuery(String accountNumber, LocalDate from, LocalDate to) {}

    // --- 출력(Result) ---
    public record GeneralResult(
            String TxExternalId,
            String accountNumber,
            String type,
            Long amount,
            Long balanceAfter,
            String status,
            LocalDateTime createdAt
    ) {}

    public record TransferResult(
            String TxExternalId,
            String fromAccountNumber,
            String toAccountNumber,
            Long amount,
            Long balanceAfter,
            String status,
            LocalDateTime createdAt
    ) {}
}
