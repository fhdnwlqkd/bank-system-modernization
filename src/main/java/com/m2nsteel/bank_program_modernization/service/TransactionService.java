package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.Account;
import com.m2nsteel.bank_program_modernization.domain.Transaction;
import com.m2nsteel.bank_program_modernization.domain.TransactionItem;
import com.m2nsteel.bank_program_modernization.domain.constant.AccountStatus;
import com.m2nsteel.bank_program_modernization.repository.transaction.TransactionRepository;
import com.m2nsteel.bank_program_modernization.service.listener.BalanceSyncEvent;
import com.m2nsteel.bank_program_modernization.usecase.TransactionUsecase;
import com.m2nsteel.bank_program_modernization.service.mapper.TransactionMapper;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@NullMarked
@Transactional(readOnly = true)
public class TransactionService {

    private final IdempotencyKeyService idempotencyKeyService;
    private final PasswordEncoder passwordEncoder;
    private final TransactionRepository transactionRepository;
    private final RedisAccountService redisAccountService;
    private final AccountQueryService accountQueryService;
    private final TransactionMapper transactionMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 입금 처리 (Deposit)
     */
    @Transactional
    public TransactionUsecase.GeneralResult deposit(TransactionUsecase.DepositCommand command, String memberExternalId) {
        // 1. 멱등성 검증
        if(idempotencyKeyService.isDuplicate(command.idempotencyKey())){
            throw new BusinessException(ErrorCode.REPEATED_REQUEST);
        }

        // 2. 검증 및 조회 (나의 계좌인지 확인)
        Account account = accountQueryService.getAccountByNumber(command.accountNumber());

        // 3. 비즈니스 로직 수행
        Long newBalance = redisAccountService.updateBalance(account.getId(), command.amount(), true);

        // 4. 기록 저장
        Transaction transaction = Transaction.createDeposit(command.amount(), command.idempotencyKey());
        TransactionItem item = TransactionItem.createDepositItem(transaction, account, command.amount(), 1, newBalance);

        transactionRepository.save(transaction);
        return transactionMapper.toResult(transaction, item);
    }

    /**
     * 출금 처리 (Withdraw)
     */
    @Transactional
    public TransactionUsecase.GeneralResult withdraw(TransactionUsecase.WithdrawCommand command, String memberExternalId) {
        // 1. 멱등성 검증
        if(idempotencyKeyService.isDuplicate(command.idempotencyKey())){
            throw new BusinessException(ErrorCode.REPEATED_REQUEST);
        }

        // 2. 검증 및 조회
        Account account = accountQueryService.getAccountByNumber(command.accountNumber());
        verifyAccountPassword(command.accountPassword(), account.getAccountPassword());

        // 3. 비즈니스 로직 (잔액 부족 시 엔티티 내부에서 Exception 발생)
        Long newBalance = redisAccountService.updateBalance(account.getId(), command.amount(), false);
        if(newBalance < 0){
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        // 4. 기록 저장
        Transaction transaction = Transaction.createWithdrawal(command.amount(), command.idempotencyKey());
        TransactionItem item = TransactionItem.createWithdrawalItem(transaction, account, command.amount(), 1, newBalance);

        transactionRepository.save(transaction);

        return transactionMapper.toResult(transaction, item);
    }

    /**
     * 이체 처리 (Transfer)
     */
    @Transactional
    public TransactionUsecase.TransferResult transfer(TransactionUsecase.TransferCommand command, String fromMemberExternalId) {
        // 1. 멱등성 검증
        if(idempotencyKeyService.isDuplicate(command.idempotencyKey())){
            throw new BusinessException(ErrorCode.REPEATED_REQUEST);
        }

        // 2. 검증 및 조회 (출금 계좌 소유권 확인 & 입금 계좌 활성 확인)
        Account fromAccount = accountQueryService.getAccountByNumber(command.fromAccountNumber());
        Account toAccount = accountQueryService.getAccountByNumber(command.toAccountNumber());
        verifyAccountPassword(command.accountPassword(), fromAccount.getAccountPassword());

        // 3. 양측 계좌 잔액 업데이트
        Long fromNewBalance = redisAccountService.updateBalance(fromAccount.getId(), command.amount(), false);
        if (fromNewBalance < 0){
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        Long toNewBalance = redisAccountService.updateBalance(toAccount.getId(), command.amount(), true);

        // 4. 기록 저장
        Transaction transaction = Transaction.createTransfer(command.amount(), command.idempotencyKey());
        TransactionItem withdrawItem = TransactionItem.createWithdrawalItem(transaction, fromAccount, command.amount(), 1, fromNewBalance);
        TransactionItem depositItem = TransactionItem.createDepositItem(transaction, toAccount, command.amount(), 2, toNewBalance);

        transactionRepository.save(transaction);

        eventPublisher.publishEvent(new BalanceSyncEvent(fromAccount.getId(), fromNewBalance));
        eventPublisher.publishEvent(new BalanceSyncEvent(toAccount.getId(), toNewBalance));

        return transactionMapper.toTransferResult(transaction, withdrawItem, depositItem);
    }

    // --- Private Helpers ---
    private Account findActiveAccountWithOwnership(String accountNumber, String externalId) {
        Account account = findActiveAccount(accountNumber);
        if (!account.getMember().getExternalId().equals(externalId)) {
            throw new BusinessException(ErrorCode.NOT_ACCOUNT_OWNER);
        }
        return account;
    }

    private Account findActiveAccount(String accountNumber) {
        Account account = accountQueryService.getAccountByNumber(accountNumber);
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_CLOSED);
        }
        return account;
    }

    private void verifyAccountPassword(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
    }
}