package com.m2nsteel.bank_program_modernization.usecase;

import org.jspecify.annotations.NullMarked;

import java.time.LocalDate;
import java.time.LocalDateTime;

@NullMarked
public class CardUsecase {

    // --- 입력(Command) ---
    public record ChangeCardPasswordCommand(
            String oldPassword,
            String newPassword
    ) {}

    public record IssueCardCommand(
            String accountNumber,
            String accountPassword,
            String cardPassword,
            String cardType // CHECK, CREDIT 등
    ) {}

    public record UpdateCardStatusCommand(
            String cardExternalId,
            String password,
            String status
    ) {}

    public record CardPaymentCommand(
            String cardExternalId,
            Long amount,
            String password,
            String businessNumber, // 사업자 등록 번호(BRN)
            String idempotencyKey
    ) {}

    public record RefundCommand(
            String paymentExternalId, // 원본 결제 건 식별자
            Long amount,              // 환불 요청 금액
            String reason,
            String idempotencyKey
    ) {}

    // --- 출력(Result) ---
    public record CardResult(
            String externalId,
            String cardNumber,
            String accountNumber,
            String cardType,
            String status,
            LocalDate expiredAt,
            LocalDateTime createdAt
    ) {}

    public record CardPaymentResult(
            String paymentExternalId,
            String transactionExternalId,
            String maskedCardNumber,
            Long amount,
            String accountNumber,
            Long balanceAfter,
            String merchantName,
            String status,
            LocalDateTime createdAt
    ) {}

    public record PaymentSummary(
            String paymentExternalId,
            String maskedCardNumber,
            Long amount,
            Long balanceAfter,
            String merchantName,
            String status,
            LocalDateTime createdAt
    ) {}

    public record RefundResult(
            String refundExternalId,          // 이번 환불 건의 식별자
            String refundTxExternalId, // 이번 환불 거래의 식별자
            String originalPaymentExternalId,   // 원본 결제 식별자
            Long originalAmount,                // 원본 결제 금액
            Long refundAmount,                  // 이번에 환불된 금액
            Long totalRefundedAmount,           // 지금까지 환불된 총 누적 금액
            Long remainingAmount,               // 남은 결제 금액 (취소 가능 잔액)
            String reason,
            String status,
            LocalDateTime createdAt
    ) {}
}
