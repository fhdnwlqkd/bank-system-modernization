package com.m2nsteel.bank_program_modernization.domain;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.constant.AccountStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.concurrent.ThreadLocalRandom;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "accounts", indexes = {
        @Index(name = "idx_account_number", columnList = "accountNumber")
})
public class Account extends BaseEntity {

    @Column(unique = true, nullable = false, updatable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String accountPassword;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @Column(nullable = false)
    private Long balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Version // 낙관적 락을 위한 버전 필드
    private Long version;

    // --- 비즈니스 로직 ---
    public void deposit(Long amount) {
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        this.balance += amount;
    }

    public void withdraw(Long amount) {
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (this.balance < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        this.balance -= amount;
    }

    public void close() {
        this.status = AccountStatus.CLOSED;
    }

    public static Account create(String accountPassword, Member member) {
        return Account.builder()
                .accountNumber("110-" + ThreadLocalRandom.current().nextInt(100, 999) + "-" + System.currentTimeMillis() % 1000000)
                .accountPassword(accountPassword)
                .member(member)
                .balance(0L)
                .status(AccountStatus.ACTIVE)
                .build();
    }
}
