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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account cardAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_account_id", nullable = false)
    private Account merchantAccount;

    @Column(nullable = false)
    private Long amount;      // 최초 결제 금액

    @Column(nullable = false)
    private Long totalRefundedAmount;   // 현재까지 환불된 누적 금액

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;  // SUCCESS, PARTIAL_REFUNDED, REFUNDED

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Column(nullable = false, updatable = false, unique = true)
    private String idempotencyKey;

    // --- 비즈니스 로직 ---
    public void refund(Long amount) {
        if (this.totalRefundedAmount + amount > this.amount) {
            throw new BusinessException(ErrorCode.EXCEED_REFUND_AMOUNT);
        }
        this.totalRefundedAmount += amount;
        this.status = (this.totalRefundedAmount.equals(this.amount))
                ? PaymentStatus.REFUNDED : PaymentStatus.PARTIAL_REFUNDED;
    }

    public Long getRefundableAmount() {
        return this.amount - this.totalRefundedAmount;
    }

    public static Payment create(Card card, Account cardAccount, Merchant merchant, Account merchantAccount, Transaction transaction, Long amount, String idempotencyKey) {
        return Payment.builder()
                .card(card)
                .cardAccount(cardAccount)
                .merchant(merchant)
                .merchantAccount(merchantAccount)
                .transaction(transaction)
                .amount(amount)
                .totalRefundedAmount(0L)
                .idempotencyKey(idempotencyKey)
                .build();
    }
}
