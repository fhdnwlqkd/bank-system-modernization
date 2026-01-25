package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.dto.request.AccountCreateRequest;
import com.m2nsteel.bank_program_modernization.dto.request.DepositRequest;
import com.m2nsteel.bank_program_modernization.dto.request.MemberSignUpRequest;
import com.m2nsteel.bank_program_modernization.dto.response.AccountResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Transactional
class DepositServiceTest {
    @Autowired TransactionService transactionService;
    @Autowired AccountService accountService;
    @Autowired MemberService memberService;

    @Test
    @DisplayName("입금 성공 테스트")
    void deposit() {
        // 1. Given: 테스트를 위한 준비 (회원 가입, 계좌 생성)
        MemberSignUpRequest signUpRequest = new MemberSignUpRequest(
                "testuser", "password123", "M123456", "John Doe", 1L
        );
        var memberResponse = memberService.signUp(signUpRequest);
        Long branchId = 1L;
        AccountCreateRequest accountRequest = new AccountCreateRequest(memberResponse.memberId(), branchId, "1234");
        AccountResponse accountResponse = accountService.createAccount(accountRequest);
        String requestId = "unique-request-id-123";
        long depositAmount = 5000L;

        // 2. When: 입금 요청 수행
        DepositRequest depositRequest = new DepositRequest(
                requestId,
                accountResponse.accountNumber(),
                depositAmount
        );
        var response = transactionService.deposit(depositRequest);

        // 3. Then: 결과 검증
        assertEquals(depositAmount,response.amount());
        assertEquals(response.balanceAfter(), depositAmount);
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
        String requestId = "unique-request-id-123";
        long depositAmount = 5000L;
        DepositRequest depositRequest = new DepositRequest(
                requestId,
                accountResponse.accountNumber(),
                depositAmount
        );
        var transactionResponse = transactionService.deposit(depositRequest);

        // 2. When & Then: 동일한 요청 ID로 중복 입금 요청 시도
        assertThrows(BusinessException.class, () -> {
            transactionService.deposit(depositRequest);
        });
    }

    @Test
    @DisplayName("잘못된 입금액 테스트")
    void invalidDepositAmount() {
        MemberSignUpRequest signUpRequest = new MemberSignUpRequest(
                "testuser", "password123", "M123456", "John Doe", 1L
        );
        var memberResponse = memberService.signUp(signUpRequest);
        Long branchId = 1L;
        AccountCreateRequest accountRequest = new AccountCreateRequest(memberResponse.memberId(), branchId, "1234");
        AccountResponse accountResponse = accountService.createAccount(accountRequest);
        String requestId = "unique-request-id-123";
        long depositAmount = -5000L;

        // 2. When: 입금 요청 수행
        DepositRequest depositRequest = new DepositRequest(
                requestId,
                accountResponse.accountNumber(),
                depositAmount
        );
        assertThrows(BusinessException.class, () -> {
            transactionService.deposit(depositRequest);
        });
    }
}