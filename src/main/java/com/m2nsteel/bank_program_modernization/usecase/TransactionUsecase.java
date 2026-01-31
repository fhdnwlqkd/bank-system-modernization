package com.m2nsteel.bank_program_modernization.usecase;

import com.m2nsteel.bank_program_modernization.domain.constant.TransactionType;
import org.jspecify.annotations.NullMarked;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@NullMarked
public class TransactionUsecase {

    // --- 입력(Command) ---
    public record DepositCommand(String accountNumber, Long amount, String idempotencyKey) {}

    public record WithdrawCommand(String accountNumber, Long amount, String accountPassword, String idempotencyKey) {}

    public record TransferCommand(String fromAccountNumber, String toAccountNumber, Long amount, String accountPassword, String idempotencyKey) {}

    public record TransactionSearchCondition(
            String accountExternalId,
            LocalDate startDate,
            LocalDate endDate,
            TransactionType type,
            Long minAmount,
            Long maxAmount,
            Long lastId
    ) {}

    // --- 출력(Result) ---
    public record GeneralResult(
            String txExternalId,
            String accountNumber,
            String type,
            Long amount,
            Long balanceAfter,
            String status,
            boolean isRepeated,
            LocalDateTime createdAt
    ) {}

    public record TransferResult(
            String txExternalId,
            String fromAccountNumber,
            String toAccountNumber,
            Long amount,
            Long balanceAfter,
            String status,
            boolean isRepeated,
            LocalDateTime createdAt
    ) {}

    public record TransactionHistoryResult(
            String txExternalId,       // 거래 식별자 (조회/상세용)
            String type,               // 거래 유형 (입금, 출금, 결제 등)
            Long amount,               // 거래 총액
            Long delta,                // 내 계좌의 실제 변동 금액 (입금은 +, 출금은 -)
            Long balanceAfter,         // 이 거래 직후의 내 계좌 잔액
            LocalDateTime createdAt    // 거래 일시
    ) {}
}
