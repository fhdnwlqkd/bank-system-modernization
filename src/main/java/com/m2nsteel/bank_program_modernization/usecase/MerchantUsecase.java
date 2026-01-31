package com.m2nsteel.bank_program_modernization.usecase;

import com.m2nsteel.bank_program_modernization.domain.constant.PaymentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class MerchantUsecase {
    public record MerchantPaymentSearchCondition(
            String merchantExternalId,
            PaymentStatus status, // SUCCESS, REFUNDED 등
            LocalDate from,
            LocalDate to,
            Long lastId,         // 커서 (Payment ID)
            int size
    ) {}

    public record MerchantSalesSummary(
            Long totalSalesAmount,   // 총 매출액
            Long totalTransactionCount, // 총 거래 건수
            Long totalRefundAmount,  // 총 환불액 (필요 시)
            Long netAmount           // 순 매출 (Sales - Refund)
    ) {}

    public record MerchantPaymentResult(
            Long paymentId,
            String externalPaymentId,
            Long amount,
            PaymentStatus status,
            LocalDateTime createdAt,
            String cardMaskedNumber
    ) {}
}
