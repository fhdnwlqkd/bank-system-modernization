package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.domain.Member;
import com.m2nsteel.bank_program_modernization.dto.request.AccountCreateRequest;
import com.m2nsteel.bank_program_modernization.dto.request.MemberSignUpRequest;
import com.m2nsteel.bank_program_modernization.dto.response.AccountResponse;
import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import com.m2nsteel.bank_program_modernization.repository.MemberRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Transactional
class AccountServiceTest {

    @Autowired AccountService accountService;
    @Autowired MemberService memberService;
    @Test
    @DisplayName("계좌 생성 성공")
    void createAccount_success() {
        // 1. Given: 테스트를 위한 준비 (회원 가입)
        MemberSignUpRequest signUpRequest = new MemberSignUpRequest(
                "testuser", "password123", "M123456", "John Doe", 1L
        );
        var memberResponse = memberService.signUp(signUpRequest);

        // 2. When: 계좌 생성 수행
        Long branchId = 1L;
        AccountCreateRequest accountRequest = new AccountCreateRequest(memberResponse.memberId(), branchId, "1234");
        AccountResponse accountResponse = accountService.createAccount(accountRequest);

        // 3. Then: 결과 검증
        assertThat(accountResponse.memberId()).isEqualTo(memberResponse.memberId());
        assertThat(accountResponse.balance()).isEqualTo(0L);
        assertThat(accountResponse.accountNumber()).isNotBlank();
    }

    @Test
    @DisplayName("존재하지 않는 회원으로 계좌 생성 시 예외 발생")
    void createAccount_fail_memberNotFound() {
        // given: 저장되지 않은 임의의 ID 999L
        AccountCreateRequest request = new AccountCreateRequest(999L, 0L, "1234");

        // when & then
        assertThatThrownBy(() -> accountService.createAccount(request))
                .isInstanceOf(BusinessException.class);
    }
}