package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.Account;
import com.m2nsteel.bank_program_modernization.domain.Transaction;
import com.m2nsteel.bank_program_modernization.domain.TransactionItem;
import com.m2nsteel.bank_program_modernization.domain.constant.AccountStatus;
import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import com.m2nsteel.bank_program_modernization.repository.TransactionRepository;
import com.m2nsteel.bank_program_modernization.usecase.TransactionUsecase;
import com.m2nsteel.bank_program_modernization.usecase.mapper.TransactionMapper;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@NullMarked
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionMapper transactionMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 입금 처리 (Deposit)
     */
    @Transactional
    public TransactionUsecase.GeneralResult deposit(TransactionUsecase.DepositCommand command, String loginId) {
        // 1. 검증 및 조회
        Account account = findActiveAccountWithOwnership(command.accountNumber(), loginId);

        // 2. 비즈니스 로직 수행
        account.deposit(command.amount());

        // 3. 기록 생성
        Transaction transaction = Transaction.createDeposit(command.amount(), command.idempotencyKey());
        TransactionItem item = TransactionItem.createDepositItem(transaction, account, command.amount(), 1);

        transactionRepository.save(transaction);
        return transactionMapper.toResult(transaction, item);
    }

    /**
     * 출금 처리 (Withdraw)
     */
    @Transactional
    public TransactionUsecase.GeneralResult withdraw(TransactionUsecase.WithdrawCommand command, String loginId) {
        Account account = findActiveAccountWithOwnership(command.accountNumber(), loginId);
        verifyAccountPassword(command.accountPassword(), account.getAccountPassword());

        // 비즈니스 로직 (잔액 부족 시 Exception 발생 -> 자동 롤백)
        account.withdraw(command.amount());

        Transaction transaction = Transaction.createWithdrawal(command.amount(), command.idempotencyKey());
        TransactionItem item = TransactionItem.createWithdrawalItem(transaction, account, command.amount(), 1);

        transactionRepository.save(transaction);
        return transactionMapper.toResult(transaction, item);
    }

    /**
     * 이체 처리 (Transfer)
     */
    @Transactional
    public TransactionUsecase.TransferResult transfer(TransactionUsecase.TransferCommand command, String loginId) {
        Account fromAccount = findActiveAccountWithOwnership(command.fromAccountNumber(), loginId);
        Account toAccount = findActiveAccount(command.toAccountNumber());
        verifyAccountPassword(command.accountPassword(), fromAccount.getAccountPassword());

        // 양측 계좌 잔액 업데이트
        fromAccount.withdraw(command.amount());
        toAccount.deposit(command.amount());

        Transaction transaction = Transaction.createTransfer(command.amount(), command.idempotencyKey());
        TransactionItem withdrawItem = TransactionItem.createWithdrawalItem(transaction, fromAccount, command.amount(), 1);
        TransactionItem depositItem = TransactionItem.createDepositItem(transaction, toAccount, command.amount(), 2);

        transactionRepository.save(transaction);
        return transactionMapper.toTransferResult(transaction, withdrawItem, depositItem);
    }

    // --- Private Helpers ---

    private Account findActiveAccountWithOwnership(String accountNumber, String loginId) {
        Account account = findActiveAccount(accountNumber);
        if (!account.getMember().getLoginId().equals(loginId)) {
            throw new BusinessException(ErrorCode.NOT_ACCOUNT_OWNER);
        }
        return account;
    }

    private Account findActiveAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .filter(a -> a.getStatus() == AccountStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    private void verifyAccountPassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
    }
}