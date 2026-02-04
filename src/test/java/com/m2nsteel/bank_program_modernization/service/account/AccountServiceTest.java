package com.m2nsteel.bank_program_modernization.service.account;

import com.m2nsteel.bank_program_modernization.TestRedisConfig;
import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.Member;
import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import com.m2nsteel.bank_program_modernization.repository.MemberRepository;
import com.m2nsteel.bank_program_modernization.service.AccountService;
import com.m2nsteel.bank_program_modernization.usecase.AccountUsecase;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Transactional
@Import(TestRedisConfig.class)
class AccountServiceTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AccountRepository accountRepository;

    private Member savedMember;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @AfterEach
    void tearDown() {
        Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection().serverCommands().flushAll();
    }
    @BeforeEach
    void setUp() {
        // 테스트용 회원 미리 생성
        Member member = Member.create("tester1", "password", "홍길동", "010-1234-5678");
        savedMember = memberRepository.save(member);
    }

    @Nested
    @DisplayName("계좌 개설 검증")
    class CreateAccount {
        @Test
        @DisplayName("성공: 16자리 계좌번호, 초기 잔액 0원")
        void createAccount_Success() {
            // given
            var command = new AccountUsecase.AccountCreateCommand(
                    savedMember.getExternalId(),
                    "1234"
            );

            // when
            var result = accountService.createAccount(command);

            // then
            assertThat(result).isNotNull();
            assertThat(result.externalId()).isNotNull();
            assertThat(result.balance()).isZero();
            assertThat(result.status()).isEqualTo("ACTIVE");

            // 계좌번호 포맷 검증 (110-XXX-XXXXXXXXXX)
            assertThat(result.accountNumber()).matches("^110-\\d{3}-\\d{10}$");

            // DB 실제 저장 여부 확인
            var account = accountRepository.findByAccountNumber(result.accountNumber()).orElseThrow();
            assertThat(account.getMember().getId()).isEqualTo(savedMember.getId());
        }

        @Test
        @DisplayName("실패: 존재하지 않는 Member로 개설 요청 시 MEMBER_NOT_FOUND 예외 발생")
        void createAccount_MemberNotFound_ThrowsException() {
            // given
            String invalidExternalId = "non-existent-id";
            var command = new AccountUsecase.AccountCreateCommand(
                    invalidExternalId, "1234"
            );

            // when & then
            assertThatThrownBy(() -> accountService.createAccount(command))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
        }
    }

    @Test
    @DisplayName("연속 개설 시 계좌번호 중복 확인")
    void createAccount_UniqueAccountNumber() {
        // given
        var command1 = new AccountUsecase.AccountCreateCommand(savedMember.getExternalId(), "1234");
        var command2 = new AccountUsecase.AccountCreateCommand(savedMember.getExternalId(), "1234");

        // when
        var result1 = accountService.createAccount(command1);
        var result2 = accountService.createAccount(command2);

        // then
        assertThat(result1.accountNumber()).isNotEqualTo(result2.accountNumber());
    }
}
