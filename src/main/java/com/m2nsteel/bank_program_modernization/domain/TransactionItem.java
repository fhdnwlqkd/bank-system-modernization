package com.m2nsteel.bank_program_modernization.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "transaction_items", indexes = {})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransactionItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false)
    private Long delta;

    @Column(nullable = false)
    private Long balanceAfter;

    @Column(nullable = false)
    private Integer itemOrder;

    @Column(nullable = false, updatable = false)
    private LocalDateTime occurredAt;
}
