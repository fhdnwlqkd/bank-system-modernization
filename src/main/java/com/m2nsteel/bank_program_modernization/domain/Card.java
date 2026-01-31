package com.m2nsteel.bank_program_modernization.domain;

import com.m2nsteel.bank_program_modernization.domain.constant.CardStatus;
import com.m2nsteel.bank_program_modernization.domain.constant.CardType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "cards", indexes = {
        @Index(name = "idx_card_number", columnList = "cardNumber")
})
public class Card extends BaseEntity {

    @Column(unique = true, nullable = false, updatable = false)
    private String cardNumber;

    @Column(nullable = false)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardType cardType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CardStatus status;

    private LocalDate expiredAt;

    // --- 비즈니스 로직 ---
    public void changeStatus(CardStatus newStatus) {
        this.status = newStatus;
    }

    public void changePassword(String newPassword) {
        this.password = newPassword;
    }

    public static Card create(
            Account account,
            String cardNumber,
            String password,
            CardType cardType,
            LocalDate expiredAt
    ) {
        return Card.builder()
                .account(account)
                .password(password)
                .cardNumber(cardNumber)
                .cardType(cardType)
                .status(CardStatus.ACTIVE)
                .expiredAt(expiredAt)
                .build();
    }
}
