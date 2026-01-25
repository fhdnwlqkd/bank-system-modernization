package com.m2nsteel.bank_program_modernization.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@Table(name = "transaction_items", indexes = {})
public class TransactionItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false)
    private Long delta;

    @Column(nullable = false)
    private Long balanceAfter;

    @Column(nullable = false)
    private Integer itemOrder;

    @Column(nullable = false, updatable = false)
    private LocalDateTime occurredAt;
}
