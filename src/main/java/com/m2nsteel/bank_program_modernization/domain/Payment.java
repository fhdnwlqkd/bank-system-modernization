package com.m2nsteel.bank_program_modernization.domain;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.constant.PaymentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {
    @Column(nullable = false, updatable = false, unique = true)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_account_id", nullable = false)
    private Account merchantAccount;

    @Column(nullable = false)
    private Long amount;      // 최초 결제 금액

    @Column(nullable = false)
    private Long refundedAmount;   // 현재까지 환불된 누적 금액

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;  // SUCCESS, PARTIAL_REFUNDED, REFUNDED

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    // --- 비즈니스 로직 ---
    public void refund(Long amount) {
        if (this.refundedAmount + amount > this.amount) {
            throw new BusinessException(ErrorCode.EXCEED_REFUND_AMOUNT);
        }
        this.refundedAmount += amount;
        this.status = (this.refundedAmount.equals(this.amount))
                ? PaymentStatus.REFUNDED : PaymentStatus.PARTIAL_REFUNDED;
    }

    public static Payment create(Card card, Account merchantAccount, Transaction transaction, Long amount, String idempotencyKey) {
        return Payment.builder()
                .card(card)
                .merchantAccount(merchantAccount)
                .transaction(transaction)
                .amount(amount)
                .refundedAmount(0L)
                .idempotencyKey(idempotencyKey)
                .build();
    }
}
}
