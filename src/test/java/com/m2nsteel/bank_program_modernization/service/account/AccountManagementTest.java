package com.m2nsteel.bank_program_modernization.service.account;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.Account;
import com.m2nsteel.bank_program_modernization.domain.constant.AccountStatus;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Transactional
class AccountManagementTest {

    @Autowired private AccountService accountService;
    @Autowired private MemberService memberService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String memberId;
    private String accountId;
    private final String OLD_PASS = "old-pass-123!";
    private final String NEW_PASS = "new-pass-456!";

    @BeforeEach
    void setUp() {
        // 1. Given: 사용자 가입 및 계좌 생성
        var member = memberService.signUp(new MemberUsecase.MemberSignUpCommand("mincheol", "member-pass", "서민철", "010-1234-5678"));
        memberId = member.externalId();

        var account = accountService.createAccount(new AccountUsecase.AccountCreateCommand(memberId, OLD_PASS));
        accountId = account.externalId();
    }

    @Test
    @DisplayName("성공: 비밀번호 변경 - 기존 비번 검증 통과 및 새 비번 암호화 저장")
    void changePassword_Success() {
        // 2. When: 비밀번호 변경 요청
        var command = new AccountUsecase.AccountChangePasswordCommand(OLD_PASS, NEW_PASS);
        accountService.changePassword(command, accountId, memberId);

        // 3. Then: DB에서 변경된 비밀번호 확인
        Account account = accountRepository.findByExternalId(accountId).orElseThrow();
        assertThat(passwordEncoder.matches(NEW_PASS, account.getAccountPassword())).isTrue();
        assertThat(passwordEncoder.matches(OLD_PASS, account.getAccountPassword())).isFalse();
    }

    @Test
    @DisplayName("실패: 비밀번호 변경 - 현재 비밀번호가 틀리면 BusinessException 발생")
    void changePassword_WrongCurrentPassword_Fail() {
        // 2. When & Then: 틀린 기존 비밀번호로 요청 시 에러 발생
        var command = new AccountUsecase.AccountChangePasswordCommand("wrong-pass", NEW_PASS);

        assertThatThrownBy(() -> accountService.changePassword(command, accountId, memberId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD);
    }

    @Test
    @DisplayName("성공: 계좌 정지 - 상태가 CLOSED로 변경됨")
    void close_Account_Success() {
        // 2. When: 계좌 정지(해지) 요청
        accountService.close(accountId, memberId);

        // 3. Then: 계좌 상태값 확인
        Account account = accountRepository.findByExternalId(accountId).orElseThrow();
        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
    }
}
