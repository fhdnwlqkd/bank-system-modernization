package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.dto.request.AccountCreateRequest;
import com.m2nsteel.bank_program_modernization.dto.request.DepositRequest;
import com.m2nsteel.bank_program_modernization.dto.request.MemberSignUpRequest;
import com.m2nsteel.bank_program_modernization.dto.request.WithdrawRequest;
import com.m2nsteel.bank_program_modernization.dto.response.AccountResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Transactional
class WithdrawalServiceTest {
    @Autowired TransactionService transactionService;
    @Autowired AccountService accountService;
    @Autowired MemberService memberService;

    @Test
    @DisplayName("출금 성공 테스트")
    void withdraw() {
        // 1. Given: 테스트를 위한 준비 (회원 가입, 계좌 생성)
        MemberSignUpRequest signUpRequest = new MemberSignUpRequest(
                "testuser", "password123", "M123456", "John Doe", 1L
        );
        var memberResponse = memberService.signUp(signUpRequest);
        Long branchId = 1L;
        String accountPassword = "1234";
        AccountCreateRequest accountRequest = new AccountCreateRequest(memberResponse.memberId(), branchId, accountPassword);
        AccountResponse accountResponse = accountService.createAccount(accountRequest);
        String DepositRequestId = "deposit-request-id-123";
        long depositAmount = 5000L;
        DepositRequest depositRequest = new DepositRequest(
                DepositRequestId,
                accountResponse.accountNumber(),
                depositAmount
        );
        var response1 = transactionService.deposit(depositRequest);

        String requestId = "unique-request-id-123";
        long withdrawalAmount = 5000L;

        // 2. When: 입금 요청 수행
        WithdrawRequest withdrawRequest = new WithdrawRequest(
                requestId,
                accountResponse.accountNumber(),
                withdrawalAmount,
                accountPassword
        );

        // 3. Then: 결과 검증
        var response = transactionService.withdraw(withdrawRequest);
        assertEquals(withdrawalAmount,response.amount());
        assertEquals(response.balanceAfter(), 0L);
    }

    @Test
    @DisplayName("중복 요청 테스트")
    void duplicateDeposit() {
        // 1. Given: 테스트를 위한 준비 (회원 가입, 계좌 생성, 첫 입금)
        MemberSignUpRequest signUpRequest = new MemberSignUpRequest(
                "testuser", "password123", "M123456", "John Doe", 1L
        );
        var memberResponse = memberService.signUp(signUpRequest);
        Long branchId = 1L;
        AccountCreateRequest accountRequest = new AccountCreateRequest(memberResponse.memberId(), branchId, "1234");
        AccountResponse accountResponse = accountService.createAccount(accountRequest);

        String DepositRequestId = "deposit-request-id-123";
        long depositAmount = 5000L;
        DepositRequest depositRequest = new DepositRequest(
                DepositRequestId,
                accountResponse.accountNumber(),
                depositAmount
        );
        var response1 = transactionService.deposit(depositRequest);

        String requestId = "unique-request-id-123";
        long withdrawalAmount = 5000L;
        WithdrawRequest withdrawRequest = new WithdrawRequest(
                requestId,
                accountResponse.accountNumber(),
                withdrawalAmount,
                "1234"
        );
        var transactionResponse = transactionService.withdraw(withdrawRequest);
        // 2. When & Then: 동일한 요청 ID로 중복 입금 요청 시도
        assertThrows(BusinessException.class, () -> {
            transactionService.withdraw(withdrawRequest);
        });
    }

    @Test
    @DisplayName("잘못된 출금액 테스트")
    void invalidDepositAmount() {
        // 1. Given: 테스트를 위한 준비 (회원 가입, 계좌 생성, 첫 입금)
        MemberSignUpRequest signUpRequest = new MemberSignUpRequest(
                "testuser", "password123", "M123456", "John Doe", 1L
        );
        var memberResponse = memberService.signUp(signUpRequest);
        Long branchId = 1L;
        AccountCreateRequest accountRequest = new AccountCreateRequest(memberResponse.memberId(), branchId, "1234");
        AccountResponse accountResponse = accountService.createAccount(accountRequest);

        String requestId = "unique-request-id-123";
        long withdrawalAmount = 5000L;
        WithdrawRequest withdrawRequest = new WithdrawRequest(
                requestId,
                accountResponse.accountNumber(),
                withdrawalAmount,
                "1234"
        );
        // 2. When & Then: 잘못된 출금액 요청 시도
        assertThrows(BusinessException.class, () -> {
            transactionService.withdraw(withdrawRequest);
        });
    }
}