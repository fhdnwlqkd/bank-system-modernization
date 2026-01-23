package com.m2nsteel.bank_program_modernization.domain;

import com.m2nsteel.bank_program_modernization.domain.status.CardStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "cards", indexes = {})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Card {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private String cardNum;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardStatus status;

    private String expiryDate; // MM/YY
}
