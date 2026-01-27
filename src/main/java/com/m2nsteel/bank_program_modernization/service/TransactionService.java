package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.Account;
import com.m2nsteel.bank_program_modernization.domain.Member;
import com.m2nsteel.bank_program_modernization.domain.Transaction;
import com.m2nsteel.bank_program_modernization.domain.TransactionItem;
import com.m2nsteel.bank_program_modernization.domain.constant.AccountStatus;
import com.m2nsteel.bank_program_modernization.domain.constant.TransactionStatus;
import com.m2nsteel.bank_program_modernization.domain.constant.TransactionType;
import com.m2nsteel.bank_program_modernization.domain.validator.AccountValidator;
import com.m2nsteel.bank_program_modernization.domain.validator.IdempotencyValidator;
import com.m2nsteel.bank_program_modernization.domain.validator.MemberValidator;
import com.m2nsteel.bank_program_modernization.domain.validator.PasswordValidator;
import com.m2nsteel.bank_program_modernization.dto.request.DepositRequest;
import com.m2nsteel.bank_program_modernization.dto.request.TransferRequest;
import com.m2nsteel.bank_program_modernization.dto.request.WithdrawRequest;
import com.m2nsteel.bank_program_modernization.dto.response.TransactionResponse;
import com.m2nsteel.bank_program_modernization.dto.response.TransferResponse;
import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import com.m2nsteel.bank_program_modernization.repository.MemberRepository;
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
    private final IdempotencyValidator idempotencyValidator;
    private final AccountValidator accountValidator;
    private final MemberValidator memberValidator;
    private final PasswordValidator passwordValidator;

    /*
    입금 처리(Deposit)
     */
    @Transactional
    public TransactionResponse deposit(DepositRequest request, String loginId) {
        // 1. Idempotency Key 중복 체크
        idempotencyValidator.verify(request.requestId());

        // 2. 계좌 조회, 회원 조회 및 검증
        Member member = memberValidator.getActiveMember(loginId);
        Account account = accountValidator.getActiveAccount(request.accountNumber());
        accountValidator.verifyOwner(account, member.getId());

        // 3. 계좌 잔액 업데이트
        account.deposit(request.amount());

        // 4. Transaction header 생성
        Transaction transaction = createTransaction(
                TransactionType.DEPOSIT,
                request.amount(),
                request.requestId());

        // 5. Transaction Item 생성
        TransactionItem item = createTransactionItem(
                transaction,
                account,
                request.amount(),
                1);

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
    public TransactionResponse withdraw(WithdrawRequest request, String loginId) {
        // 1. Idempotency Key 중복 체크
        idempotencyValidator.verify(request.requestId());

        // 2. 계좌 조회 및 회원 조회
        Member member = memberValidator.getActiveMember(loginId);
        Account account = accountValidator.getActiveAccount(request.accountNumber());
        accountValidator.verifyOwner(account, member.getId());

        // 3. 비밀번호 검증
        passwordValidator.verify(request.accountPassword(), account.getAccountPassword());

        // 4. 계좌 잔액 업데이트 (내부에서 검증 로직 수행)
        account.withdraw(request.amount());

        // 5. Transaction header 생성
        Transaction transaction = createTransaction(
                TransactionType.WITHDRAWAL,
                request.amount(),
                request.requestId());

        // 6. Transaction Item 생성
        TransactionItem item = createTransactionItem(
                transaction,
                account,
                -request.amount(),
                1);

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
    public TransferResponse transfer(TransferRequest request, String loginId) {
        // 1. Idempotency Key 중복 체크
        idempotencyValidator.verify(request.requestId());

        // 2. 두 계좌 조회 및 검증 (보내는 사람, 받는 사람), 회원 조회 및 검증
        Member member = memberValidator.getActiveMember(loginId);
        // 보내는 계좌 검증 (소유주 확인 포함)
        Account fromAccount = accountValidator.getActiveAccount(request.fromAccountNumber());
        accountValidator.verifyOwner(fromAccount, member.getId());

        // 받는 계좌 검증 (상태만 확인)
        Account toAccount = accountValidator.getActiveAccount(request.toAccountNumber());

        // 3. 비밀번호 검증
        passwordValidator.verify(request.accountPassword(), fromAccount.getAccountPassword());

        // 4. 출금 및 입금
        fromAccount.withdraw(request.amount());
        toAccount.deposit(request.amount());

        // 5. Transaction Header 생성
        Transaction transaction = createTransaction(
                TransactionType.TRANSFER,
                request.amount(),
                request.requestId());

        // 6. Transaction Items 생성 (2개: 출금 내역, 입금 내역)
        TransactionItem withdrawItem = createTransactionItem(
                transaction,
                fromAccount,
                -request.amount(),
                1);

        TransactionItem depositItem = createTransactionItem(
                transaction,
                toAccount,
                request.amount(),
                2);

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

    // --- 엔티티 생성 Helper 메서드 ---

    private Transaction createTransaction(TransactionType type, Long amount, String requestId) {
        return Transaction.builder()
                .type(type)
                .status(TransactionStatus.SUCCESS)
                .amount(amount)
                .requestId(requestId)
                .occurredAt(LocalDateTime.now())
                .build();
    }

    private TransactionItem createTransactionItem(Transaction transaction, Account account, Long delta, int order) {
        TransactionItem item = TransactionItem.builder()
                .transaction(transaction)
                .account(account)
                .delta(delta)
                .balanceAfter(account.getBalance())
                .itemOrder(order)
                .occurredAt(transaction.getOccurredAt())
                .build();
        transaction.addItem(item);
        return item;
    }
}
