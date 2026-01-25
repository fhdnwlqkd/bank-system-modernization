package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.dto.request.*;
import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
@SpringBootTest
public class TransferServiceTest {
    @Autowired TransactionService transactionService;
    @Autowired AccountService accountService;
    @Autowired MemberService memberService;
    @Autowired AccountRepository accountRepository;
    @Autowired BranchService branchService;

    private String senderAccountNum;
    private String receiverAccountNum;
    private final String accountPassword = "password1234";

    @BeforeEach
    void setUp() {
        // 1. 지점 생성
        var branch = branchService.createBranch(
                new BranchCreateRequest(
                        "Test Branch",
                        "123 Test St",
                        "555-0000"
                )
        );

        // 2. 보내는 사람 가입 및 계좌 생성
        var sender = memberService.signUp(new MemberSignUpRequest("sender", "p1", "Sender", branch.branchCode()));
        var senderAccount = accountService.createAccount(new AccountCreateRequest(sender.memberNumber(), branch.branchCode(), accountPassword));
        senderAccountNum = senderAccount.accountNumber();

        // 3. 받는 사람 가입 및 계좌 생성
        var receiver = memberService.signUp(new MemberSignUpRequest("receiver", "p2", "Receiver", branch.branchCode()));
        var receiverAccount = accountService.createAccount(new AccountCreateRequest(receiver.memberNumber(), branch.branchCode(), "any-pw"));
        receiverAccountNum = receiverAccount.accountNumber();

        // 4. 보내는 사람에게 초기 잔액 10,000원 입금
        transactionService.deposit(new DepositRequest(UUID.randomUUID().toString(), senderAccountNum, 10000L));
    }

    @Test
    @DisplayName("송금 성공 테스트: 잔액 이동 확인")
    void transfer_success() {
        // Given
        long transferAmount = 5000L;
        TransferRequest request = new TransferRequest(
                UUID.randomUUID().toString(),
                senderAccountNum,
                receiverAccountNum,
                accountPassword,
                transferAmount
        );

        // When
        var response = transactionService.transfer(request);

        // Then
        assertThat(response.amount()).isEqualTo(transferAmount);
        assertThat(response.balanceAfter()).isEqualTo(5000L);

        // 받는 사람 잔액 확인
        var receiverAccount = accountRepository.findByAccountNumber(receiverAccountNum).orElseThrow();
        assertThat(receiverAccount.getBalance()).isEqualTo(5000L);
    }

    @Test
    @DisplayName("송금 실패: 잔액 부족")
    void transfer_fail_insufficient_balance() {
        // Given: 20,000원 송금 시도 (잔액은 10,000원뿐)
        TransferRequest request = new TransferRequest(
                UUID.randomUUID().toString(),
                senderAccountNum,
                receiverAccountNum,
                accountPassword,
                20000L
        );

        // When & Then
        assertThrows(BusinessException.class, () -> transactionService.transfer(request));
    }

    @Test
    @DisplayName("송금 실패: 비밀번호 불일치")
    void transfer_fail_invalid_password() {
        // Given: 틀린 비밀번호 입력
        TransferRequest request = new TransferRequest(
                UUID.randomUUID().toString(),
                senderAccountNum,
                receiverAccountNum,
                "wrong-password",
                5000L
        );

        // When & Then
        assertThrows(BusinessException.class, () -> transactionService.transfer(request));
    }

    @Test
    @DisplayName("송금 실패: 중복 요청")
    void transfer_fail_duplicate_request() {
        // Given
        String requestId = "duplicate-id";
        TransferRequest request = new TransferRequest(
                requestId,
                senderAccountNum,
                receiverAccountNum,
                accountPassword,
                1000L
        );
        transactionService.transfer(request); // 첫 번째 요청

        // When & Then: 동일한 requestId로 다시 요청
        assertThrows(BusinessException.class, () -> transactionService.transfer(request));
    }
}
