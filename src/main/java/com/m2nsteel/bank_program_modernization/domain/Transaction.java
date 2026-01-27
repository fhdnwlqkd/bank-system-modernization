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

    // 중복 요청 방지
    @Column(nullable = false, updatable = false, unique = true)
    private String idempotencyKey;

    @Builder.Default
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL)
    private List<TransactionItem> items = new ArrayList<>();

    // 아이템 추가 및 연관관계 편의 메서드
    public void addItem(TransactionItem item) {
        this.items.add(item);
    }

    public void complete() {
        this.status = TransactionStatus.SUCCESS;
    }

    public void fail(String reason) {
        this.status = TransactionStatus.FAILED;
        this.failReason = reason;
    }
}
