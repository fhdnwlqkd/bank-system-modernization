package com.m2nsteel.bank_program_modernization.service.account;

import com.m2nsteel.bank_program_modernization.TestRedisConfig;
import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.constant.AccountStatus;
import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import com.m2nsteel.bank_program_modernization.service.AccountService;
import com.m2nsteel.bank_program_modernization.service.MemberService;
import com.m2nsteel.bank_program_modernization.usecase.AccountUsecase;
import com.m2nsteel.bank_program_modernization.usecase.MemberUsecase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Transactional
@Import(TestRedisConfig.class)
class AccountManagementTest {

    @Autowired private AccountService accountService;
    @Autowired private MemberService memberService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String memberId;
    private String accountId;
    private final String OLD_PASS = "old-pass-123!";
    private final String NEW_PASS = "new-pass-456!";
    @Autowired
    private StringRedisTemplate redisTemplate;

    @AfterEach
    void tearDown() {
        Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection().serverCommands().flushAll();
    }
    @BeforeEach
    void setUp() {
        // 1. Given: 사용자 가입 및 계좌 생성
        var member = memberService.signUp(new MemberUsecase.MemberSignUpCommand("gildong", "member-pass", "홍길동", "010-1234-5678"));
        memberId = member.externalId();

        var account = accountService.createAccount(new AccountUsecase.AccountCreateCommand(memberId, OLD_PASS));
        accountId = account.externalId();
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
        String status = accountService.getAccountDetail(accountId, memberId).status();
        assertThat(status).isEqualTo(AccountStatus.CLOSED.toString());
    }
}
