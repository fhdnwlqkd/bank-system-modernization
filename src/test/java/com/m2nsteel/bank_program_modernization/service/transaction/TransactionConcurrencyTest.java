package com.m2nsteel.bank_program_modernization.service.transaction;

import com.m2nsteel.bank_program_modernization.domain.Account;
import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import com.m2nsteel.bank_program_modernization.repository.MemberRepository;
import com.m2nsteel.bank_program_modernization.service.AccountService;
import com.m2nsteel.bank_program_modernization.service.MemberService;
import com.m2nsteel.bank_program_modernization.service.TransactionService;
import com.m2nsteel.bank_program_modernization.usecase.AccountUsecase;
import com.m2nsteel.bank_program_modernization.usecase.MemberUsecase;
import com.m2nsteel.bank_program_modernization.usecase.TransactionUsecase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
class TransactionConcurrencyTest {

    @Autowired private TransactionService transactionService;
    @Autowired private MemberService memberService;
    @Autowired private AccountService accountService;

    @Autowired private AccountRepository accountRepository;
    @Autowired private MemberRepository memberRepository;

    private String senderExternalId;
    private String receiverExternalId;
    private String senderAccountNo;
    private String receiverAccountNo;
    private final String PASSWORD = "password123!";

    @BeforeEach
    void setUp() {
        // 1. 회원 및 계좌 생성 (테스트마다 독립적인 데이터 확보)
        var sMember = memberService.signUp(new MemberUsecase.MemberSignUpCommand(
                "sender_" + System.currentTimeMillis(), PASSWORD, "송금인", "010-1111-2222"));
        var rMember = memberService.signUp(new MemberUsecase.MemberSignUpCommand(
                "receiver_" + System.currentTimeMillis(), PASSWORD, "수취인", "010-3333-4444"));

        senderExternalId = sMember.externalId();
        receiverExternalId = rMember.externalId();

        var sAcc = accountService.createAccount(new AccountUsecase.AccountCreateCommand(senderExternalId, PASSWORD));
        var rAcc = accountService.createAccount(new AccountUsecase.AccountCreateCommand(receiverExternalId, PASSWORD));

        senderAccountNo = sAcc.accountNumber();
        receiverAccountNo = rAcc.accountNumber();
    }

    @Test
    @DisplayName("동시 입금 테스트: 100명이 동시에 1,000원씩 입금하면 10만원이 되어야 한다")
    void concurrent_deposit_test() throws InterruptedException {
        int threadCount = 100;
        long depositAmount = 1_000L;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            String idempotencyKey = "dep-key-" + i;
            executorService.execute(() -> {
                try {
                    transactionService.deposit(
                            new TransactionUsecase.DepositCommand(receiverAccountNo, depositAmount, idempotencyKey),
                            receiverExternalId
                    );
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        Account finalAccount = accountRepository.findByAccountNumber(receiverAccountNo).orElseThrow();
        System.out.println("입금 결과 - 실제 잔액: " + finalAccount.getBalance());
        assertThat(finalAccount.getBalance()).isEqualTo(threadCount * depositAmount);
    }

    @Test
    @DisplayName("동시 출금 테스트: 10만원에서 100명이 동시에 1,000원씩 출금하면 0원이 되어야 한다")
    void concurrent_withdraw_test() throws InterruptedException {
        // 초기 잔액 10만원 입금 ㅋ
        transactionService.deposit(new TransactionUsecase.DepositCommand(senderAccountNo, 100_000L, "init-key"), senderExternalId);

        int threadCount = 100;
        long withdrawAmount = 1_000L;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            String idempotencyKey = "with-key-" + i;
            executorService.execute(() -> {
                try {
                    transactionService.withdraw(
                            new TransactionUsecase.WithdrawCommand(senderAccountNo, withdrawAmount, PASSWORD, idempotencyKey),
                            senderExternalId
                    );
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        Account finalAccount = accountRepository.findByAccountNumber(senderAccountNo).orElseThrow();
        System.out.println("출금 결과 - 실제 잔액: " + finalAccount.getBalance());
        assertThat(finalAccount.getBalance()).isEqualTo(0L);
    }

    @Test
    @DisplayName("동시 이체 테스트: 10만원을 100명이 동시에 1,000원씩 이체하면 수취인 잔액은 10만원이 되어야 한다")
    void concurrent_transfer_test() throws InterruptedException {
        // 송금인 초기 잔액 10만원 입금
        transactionService.deposit(new TransactionUsecase.DepositCommand(senderAccountNo, 100_000L, "trans-init"), senderExternalId);

        int threadCount = 100;
        long transferAmount = 1_000L;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            String idempotencyKey = "tr-key-" + i;
            executorService.execute(() -> {
                try {
                    transactionService.transfer(
                            new TransactionUsecase.TransferCommand(senderAccountNo, receiverAccountNo, transferAmount, PASSWORD, idempotencyKey),
                            senderExternalId
                    );
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();

        Account senderAcc = accountRepository.findByAccountNumber(senderAccountNo).orElseThrow();
        Account receiverAcc = accountRepository.findByAccountNumber(receiverAccountNo).orElseThrow();

        System.out.println("이체 결과 - 송금인 잔액: " + senderAcc.getBalance());
        System.out.println("이체 결과 - 수취인 잔액: " + receiverAcc.getBalance());

        assertThat(senderAcc.getBalance()).isEqualTo(0L);
        assertThat(receiverAcc.getBalance()).isEqualTo(100_000L);
    }
}
