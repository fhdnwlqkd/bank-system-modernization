package com.m2nsteel.bank_program_modernization.domain;

import com.m2nsteel.bank_program_modernization.domain.constant.TransactionStatus;
import com.m2nsteel.bank_program_modernization.domain.constant.TransactionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transaction_idempotency", columnList = "idempotencyKey")
})
public class Transaction extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(nullable = false, updatable = false)
    private Long amount;

    private String failReason;

    @Builder.Default
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL)
    private List<TransactionItem> items = new ArrayList<>();

    // 중복 요청 방지
    @Column(nullable = false, updatable = false, unique = true)
    private String idempotencyKey;

    // 아이템 추가 및 연관관계 편의 메서드
    public void addItem(TransactionItem item) {
        this.items.add(item);
    }

    public static Transaction createDeposit(Long amount, String idempotencyKey) {
        return create(TransactionType.DEPOSIT, amount, idempotencyKey);
    }

    public static Transaction createWithdrawal(Long amount, String idempotencyKey) {
        return create(TransactionType.WITHDRAW, amount, idempotencyKey);
    }

    public static Transaction createTransfer(Long amount, String idempotencyKey) {
        return create(TransactionType.TRANSFER, amount, idempotencyKey);
    }

    public static Transaction createPayment(Long amount, String idempotencyKey) {
        return create(TransactionType.PAYMENT, amount, idempotencyKey);
    }

    public static Transaction createRefund(Long amount, String idempotencyKey) {
        return create(TransactionType.REFUND, amount, idempotencyKey);
    }
    private static Transaction create(TransactionType type, Long amount, String idempotencyKey) {
        return Transaction.builder()
                .type(type)
                .amount(amount)
                .idempotencyKey(idempotencyKey)
                .status(TransactionStatus.SUCCESS)
                .items(new ArrayList<>())
                .build();
    }
}
