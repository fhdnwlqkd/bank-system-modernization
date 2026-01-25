package com.m2nsteel.bank_program_modernization.domain;

import com.m2nsteel.bank_program_modernization.domain.constant.CardStatus;
import com.m2nsteel.bank_program_modernization.domain.constant.CardType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@Table(name = "cards", indexes = {})
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
    private CardType cardType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardStatus status;

    private LocalDateTime issuedAt;
    private LocalDateTime expiredAt;
}
