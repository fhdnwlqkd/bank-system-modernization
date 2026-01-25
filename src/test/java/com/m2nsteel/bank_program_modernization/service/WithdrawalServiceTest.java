package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.dto.request.*;
import com.m2nsteel.bank_program_modernization.dto.response.AccountResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Transactional
class WithdrawalServiceTest {
    @Autowired TransactionService transactionService;
    @Autowired AccountService accountService;
    @Autowired MemberService memberService;
    @Autowired BranchService branchService;
    private String accountNum;
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
        // 2.가입 및 계좌 생성
        var member = memberService.signUp(new MemberSignUpRequest("member1", "p1", "Member", branch.branchCode()));
        var account = accountService.createAccount(new AccountCreateRequest(member.memberNumber(), branch.branchCode(), accountPassword));
        accountNum = account.accountNumber();

        // 3. 초기 잔액 10,000원 입금
        transactionService.deposit(new DepositRequest(UUID.randomUUID().toString(), accountNum, 10000L));
    }

    @Test
    @DisplayName("출금 성공 테스트: 잔액 확인")
    void withdraw() {
        // 1. Given: 테스트를 위한 준비 (회원 가입, 계좌 생성)
        String requestId = "unique-request-id-123";
        long withdrawalAmount = 10000L;

        // 2. When: 입금 요청 수행
        WithdrawRequest withdrawRequest = new WithdrawRequest(
                requestId,
                accountNum,
                withdrawalAmount,
                accountPassword
        );

        // 3. Then: 결과 검증
        var response = transactionService.withdraw(withdrawRequest);
        assertEquals(withdrawalAmount,response.amount());
        assertEquals(response.balanceAfter(), 0L);
    }

    @Test
    @DisplayName("출금 실패: 중복 요청 테스트")
    void duplicateDeposit() {
        // 1. Given: 테스트를 위한 준비
        String requestId = "unique-request-id-123";
        long withdrawalAmount = 5000L;
        WithdrawRequest withdrawRequest = new WithdrawRequest(
                requestId,
                accountNum,
                withdrawalAmount,
                accountPassword
        );
        var transactionResponse = transactionService.withdraw(withdrawRequest);
        // 2. When & Then: 동일한 요청 ID로 중복 입금 요청 시도
        assertThrows(BusinessException.class, () -> {
            transactionService.withdraw(withdrawRequest);
        });
    }

    @Test
    @DisplayName("출금 실패: 잘못된 출금액 테스트")
    void invalidDepositAmount() {
        // 1. Given: 테스트를 위한 준비
        String requestId = "unique-request-id-123";
        long withdrawalAmount = 20000L;
        WithdrawRequest withdrawRequest = new WithdrawRequest(
                requestId,
                accountNum,
                withdrawalAmount,
                accountPassword
        );
        // 2. When & Then: 잘못된 출금액 요청 시도
        assertThrows(BusinessException.class, () -> {
            transactionService.withdraw(withdrawRequest);
        });
    }

    @Test
    @DisplayName("출금 실패: 잘못된 비밀번호 테스트")
    void invalidAccountPassword() {
        // 1. Given: 테스트를 위한 준비
        String requestId = "unique-request-id-123";
        long withdrawalAmount = 5000L;
        WithdrawRequest withdrawRequest = new WithdrawRequest(
                requestId,
                accountNum,
                withdrawalAmount,
                "wrong-password"
        );
        // 2. When & Then: 잘못된 비밀번호 요청 시도
        assertThrows(BusinessException.class, () -> {
            transactionService.withdraw(withdrawRequest);
        });
    }

}