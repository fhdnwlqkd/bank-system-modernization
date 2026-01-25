package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.Transaction;
import com.m2nsteel.bank_program_modernization.domain.TransactionItem;
import com.m2nsteel.bank_program_modernization.domain.constant.TransactionStatus;
import com.m2nsteel.bank_program_modernization.domain.constant.TransactionType;
import com.m2nsteel.bank_program_modernization.dto.request.DepositRequest;
import com.m2nsteel.bank_program_modernization.dto.response.TransactionResponse;
import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import com.m2nsteel.bank_program_modernization.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    /*
    입금 처리(Deposit)
     */
    public TransactionResponse deposit(DepositRequest request) {
        // 1. Idempotency Key 중복 체크
        if (transactionRepository.existsByRequestId(request.requestId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_REQUEST);
        }

        // 2. 계좌 존재 여부 검증
        var account = accountRepository.findByAccountNumber(request.accountNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 3. 계좌 잔액 업데이트
        account.deposit(request.amount());

        // 4. Transaction header 생성
        Transaction transaction = Transaction.builder()
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.SUCCESS)
                .amount(request.amount())
                .requestId(request.requestId())
                .occurredAt(LocalDateTime.now())
                .build();

        // 5. Transaction Item 생성
        TransactionItem item = TransactionItem.builder()
                .transaction(transaction)
                .account(account)
                .delta(request.amount())
                .balanceAfter(account.getBalance())
                .itemOrder(1)
                .occurredAt(transaction.getOccurredAt())
                .build();
        transaction.addItem(item);

        // 6. Transaction 저장
        Transaction savedTransaction = transactionRepository.save(transaction);

        // 7. 응답 DTO 변환
        return new TransactionResponse(
                savedTransaction.getId(),
                account.getAccountNumber(),
                savedTransaction.getType().name(),
                savedTransaction.getAmount(),
                item.getBalanceAfter(),
                savedTransaction.getOccurredAt()
        );
    }
}
