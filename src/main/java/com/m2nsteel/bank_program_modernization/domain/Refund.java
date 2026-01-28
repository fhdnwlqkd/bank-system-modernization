package com.m2nsteel.bank_program_modernization.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund extends BaseEntity {
    @Column(nullable = false, updatable = false, unique = true)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(nullable = false)
    private Long refundAmount;

    private String reason;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id") // 환불로 인해 발생한 '입금' 거래 기록
    private Transaction transaction;
}
