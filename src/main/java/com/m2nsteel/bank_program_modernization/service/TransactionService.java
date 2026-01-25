package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.Transaction;
import com.m2nsteel.bank_program_modernization.domain.TransactionItem;
import com.m2nsteel.bank_program_modernization.domain.constant.TransactionStatus;
import com.m2nsteel.bank_program_modernization.domain.constant.TransactionType;
import com.m2nsteel.bank_program_modernization.dto.request.DepositRequest;
import com.m2nsteel.bank_program_modernization.dto.request.TransferRequest;
import com.m2nsteel.bank_program_modernization.dto.request.WithdrawRequest;
import com.m2nsteel.bank_program_modernization.dto.response.TransactionResponse;
import com.m2nsteel.bank_program_modernization.dto.response.TransferResponse;
import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import com.m2nsteel.bank_program_modernization.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    /*
    입금 처리(Deposit)
     */
    @Transactional
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

    /*
    출금 처리(Withdraw)
     */
    @Transactional
    public TransactionResponse withdraw(WithdrawRequest request) {
        // 1. Idempotency Key 중복 체크
        if (transactionRepository.existsByRequestId(request.requestId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_REQUEST);
        }

        // 2. 계좌 존재 여부 검증
        var account = accountRepository.findByAccountNumber(request.accountNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 3. 비밀번호 검증
        if (!passwordEncoder.matches(request.accountPassword(), account.getAccountPassword())) {
            throw new BusinessException(ErrorCode.INVALID_ACCOUNT_PASSWORD);
        }

        // 4. 계좌 잔액 업데이트 (내부에서 검증 로직 수행)
        account.withdraw(request.amount());

        // 5. Transaction header 생성
        Transaction transaction = Transaction.builder()
                .type(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.SUCCESS)
                .amount(request.amount())
                .requestId(request.requestId())
                .occurredAt(LocalDateTime.now())
                .build();

        // 6. Transaction Item 생성
        TransactionItem item = TransactionItem.builder()
                .transaction(transaction)
                .account(account)
                .delta(-request.amount())
                .balanceAfter(account.getBalance())
                .itemOrder(1)
                .occurredAt(transaction.getOccurredAt())
                .build();
        transaction.addItem(item);

        // 7. Transaction 저장
        Transaction savedTransaction = transactionRepository.save(transaction);

        // 8. 응답 DTO 변환
        return new TransactionResponse(
                savedTransaction.getId(),
                account.getAccountNumber(),
                savedTransaction.getType().name(),
                savedTransaction.getAmount(),
                item.getBalanceAfter(),
                savedTransaction.getOccurredAt()
        );
    }

    /*
    이체 처리(Transfer)
     */
    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        // 1. Idempotency Key 중복 체크
        if (transactionRepository.existsByRequestId(request.requestId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_REQUEST);
        }

        // 2. 두 계좌 존재 여부 검증 (보내는 사람, 받는 사람)
        var fromAccount = accountRepository.findByAccountNumber(request.fromAccountNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
        var toAccount = accountRepository.findByAccountNumber(request.toAccountNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 3. 비밀번호 검증
        if (!passwordEncoder.matches(request.accountPassword(), fromAccount.getAccountPassword())) {
            throw new BusinessException(ErrorCode.INVALID_ACCOUNT_PASSWORD);
        }

        // 4. 출금 및 입금
        fromAccount.withdraw(request.amount());
        toAccount.deposit(request.amount());

        // 5. Transaction Header 생성
        Transaction transaction = Transaction.builder()
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.SUCCESS)
                .amount(request.amount())
                .requestId(request.requestId())
                .occurredAt(LocalDateTime.now())
                .build();

        // 6. Transaction Items 생성 (2개: 출금 내역, 입금 내역)
        TransactionItem withdrawItem = TransactionItem.builder()
                .transaction(transaction)
                .account(fromAccount)
                .delta(-request.amount())
                .balanceAfter(fromAccount.getBalance())
                .occurredAt(transaction.getOccurredAt())
                .itemOrder(1)
                .build();

        TransactionItem depositItem = TransactionItem.builder()
                .transaction(transaction)
                .account(toAccount)
                .delta(request.amount())
                .balanceAfter(toAccount.getBalance())
                .occurredAt(transaction.getOccurredAt())
                .itemOrder(2)
                .build();

        transaction.addItem(withdrawItem);
        transaction.addItem(depositItem);

        // 7. 저장
        transactionRepository.save(transaction);

        // 8. 응답 DTO 변환
        return new TransferResponse(
                transaction.getId(),
                fromAccount.getAccountNumber(),
                toAccount.getAccountNumber(),
                transaction.getAmount(),
                withdrawItem.getBalanceAfter(),
                transaction.getStatus().name(),
                transaction.getOccurredAt()
        );
    }
}
