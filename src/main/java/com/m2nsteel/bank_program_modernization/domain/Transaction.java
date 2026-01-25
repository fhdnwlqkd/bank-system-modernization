package com.m2nsteel.bank_program_modernization.domain;

import com.m2nsteel.bank_program_modernization.domain.constant.TransactionStatus;
import com.m2nsteel.bank_program_modernization.domain.constant.TransactionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@Table(name="transactions", indexes = {})
@EntityListeners(AuditingEntityListener.class)
public class Transaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type; // DEPOSIT, WITHDRAW, TRANSFER

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status; // PENDING, SUCCESS, FAIL

    @Column(nullable = false)
    private Long amount;

    private String failReason;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @Builder.Default
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL)
    private List<TransactionItem> items = new ArrayList<>();

    // 요청 식별자 -> 중복 요청 방지용
    @Column(nullable = false, updatable = false, unique = true)
    private String requestId;

    public void addItem(TransactionItem item) {
        items.add(item);
    }
}
