package com.m2nsteel.bank_program_modernization.domain;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.constant.AccountStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "accounts", indexes = {})
@EntityListeners(AuditingEntityListener.class)
@Builder
public class Account {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(unique = true, nullable = false, updatable = false)
    String accountNumber;

    @Column(nullable = false)
    String accountPassword;

    @Column(nullable = false)
    Long memberId;

    @Column(nullable = false)
    Long balance;

    @Enumerated(EnumType.STRING)
    AccountStatus status;

    @Version
    Long version;
    Long branchId;

    @CreatedDate
    LocalDateTime createdAt;

    public void deposit(Long amount) {
        if(amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        this.balance += amount;
    }
}
