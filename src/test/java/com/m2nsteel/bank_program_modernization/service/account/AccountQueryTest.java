package com.m2nsteel.bank_program_modernization.service.account;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import com.m2nsteel.bank_program_modernization.service.AccountService;
import com.m2nsteel.bank_program_modernization.service.MemberService;
import com.m2nsteel.bank_program_modernization.usecase.AccountUsecase;
import com.m2nsteel.bank_program_modernization.usecase.MemberUsecase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@SpringBootTest
@Transactional
public class AccountQueryTest {
    @Autowired private AccountService accountService;
    @Autowired private MemberService memberService;
    @Autowired private AccountRepository accountRepository;

    private String memberAId;
    private String memberBId;
    private final String PASS = "password123!";

    @BeforeEach
    void setUp() {
        // 1. Given: 두 명의 사용자 가입
        var memberA = memberService.signUp(new MemberUsecase.MemberSignUpCommand("userA", PASS, "사용자A", "010-1111-1111"));
        var memberB = memberService.signUp(new MemberUsecase.MemberSignUpCommand("userB", PASS, "사용자B", "010-2222-2222"));
        memberAId = memberA.externalId();
        memberBId = memberB.externalId();

        // 2. Given: 사용자A는 계좌 2개 개설, 사용자B는 1개 개설
        accountService.createAccount(new AccountUsecase.AccountCreateCommand(memberAId, PASS));
        accountService.createAccount(new AccountUsecase.AccountCreateCommand(memberAId, PASS));
        accountService.createAccount(new AccountUsecase.AccountCreateCommand(memberBId, PASS));
    }

    @Test
    @DisplayName("성공: 내 모든 계좌 목록 조회")
    void getMyAccounts_Success() {
        // When: 사용자A의 계좌 목록 조회
        List<AccountUsecase.AccountResult> results = accountService.getMyAccounts(memberAId);

        // Then: A의 계좌 개수(2개)와 데이터 정합성 확인
        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("성공: 특정 계좌 상세 조회")
    void getAccountDetail_Success() {
        // Given: 사용자A의 첫 번째 계좌 ID 확보
        String accountId = accountService.getMyAccounts(memberAId).getFirst().externalId();

        // When: 상세 조회 요청
        var result = accountService.getAccountDetail(accountId, memberAId);

        // Then: 결과 확인
        assertThat(result.externalId()).isEqualTo(accountId);
        assertThat(result.balance()).isNotNull();
    }

    @Test
    @DisplayName("실패: 타인의 계좌 상세 조회 - 예외 발생")
    void getAccountDetail_Unauthorized_Fail() {
        // Given: 사용자B의 계좌 ID를 사용자A가 훔쳐본다고 가정
        String memberBAccountId = accountService.getMyAccounts(memberBId).getFirst().externalId();

        // When & Then: 사용자A가 B의 계좌를 조회하려고 하면 예외 발생
        assertThatThrownBy(() -> accountService.getAccountDetail(memberBAccountId, memberAId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ACCOUNT_OWNER);
    }
}
