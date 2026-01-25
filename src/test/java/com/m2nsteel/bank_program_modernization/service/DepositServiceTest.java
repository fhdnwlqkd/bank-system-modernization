package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.dto.request.AccountCreateRequest;
import com.m2nsteel.bank_program_modernization.dto.request.DepositRequest;
import com.m2nsteel.bank_program_modernization.dto.request.MemberSignUpRequest;
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
class DepositServiceTest {
    @Autowired TransactionService transactionService;
    @Autowired AccountService accountService;
    @Autowired MemberService memberService;

    private String accountNum;
    private final String accountPassword = "password1234";

    @BeforeEach
    void setUp() {
        // 1.가입 및 계좌 생성
        var member = memberService.signUp(new MemberSignUpRequest("member1", "p1", "M1", "Member", 1L));
        var account = accountService.createAccount(new AccountCreateRequest(member.memberId(), 1L, accountPassword));
        accountNum = account.accountNumber();
    }

    @Test
    @DisplayName("입금 성공 테스트: 잔액 확인")
    void deposit() {
        // 1. Given: 테스트를 위한 준비
        String requestId = "unique-request-id-123";
        long depositAmount = 5000L;

        // 2. When: 입금 요청 수행
        DepositRequest depositRequest = new DepositRequest(
                requestId,
                accountNum,
                depositAmount
        );
        var response = transactionService.deposit(depositRequest);

        // 3. Then: 결과 검증
        assertEquals(depositAmount,response.amount());
        assertEquals(response.balanceAfter(), depositAmount);
    }

    @Test
    @DisplayName("입금 실패: 중복 요청 테스트")
    void duplicateDeposit() {
        // 1. Given: 테스트를 위한 준비 (회원 가입, 계좌 생성, 첫 입금)
        String requestId = "unique-request-id-123";
        long depositAmount = 5000L;
        DepositRequest depositRequest = new DepositRequest(
                requestId,
                accountNum,
                depositAmount
        );
        var transactionResponse = transactionService.deposit(depositRequest);

        // 2. When & Then: 동일한 요청 ID로 중복 입금 요청 시도
        assertThrows(BusinessException.class, () -> {
            transactionService.deposit(depositRequest);
        });
    }

    @Test
    @DisplayName("입금 실패: 잘못된 입금액 테스트")
    void invalidDepositAmount() {
        String requestId = "unique-request-id-123";
        long depositAmount = -5000L;

        // 2. When: 입금 요청 수행
        DepositRequest depositRequest = new DepositRequest(
                requestId,
                accountNum,
                depositAmount
        );
        assertThrows(BusinessException.class, () -> {
            transactionService.deposit(depositRequest);
        });
    }
}