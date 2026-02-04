package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.Member;
import com.m2nsteel.bank_program_modernization.domain.constant.MemberRole;
import com.m2nsteel.bank_program_modernization.domain.constant.MemberStatus;
import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import com.m2nsteel.bank_program_modernization.repository.MemberRepository;
import com.m2nsteel.bank_program_modernization.usecase.MemberUsecase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Transactional
class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private EntityManager em;

    private static final String PASSWORD = "password123!";
    @Autowired
    private StringRedisTemplate redisTemplate;

    @AfterEach
    void tearDown() {
        Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection().serverCommands().flushAll();
    }
    @Nested
    @DisplayName("회원 가입 검증")
    class SignUpValidation {

        @Test
        @DisplayName("일반 회원 가입 성공")
        void signUp_Member_Success() {
            // given
            var command = new MemberUsecase.MemberSignUpCommand(
                    "tester1", PASSWORD, "홍길동", "010-1111-2222"
            );

            // when
            var result = memberService.signUp(command);

            System.out.println("가입 결과: " + result);
            // then
            assertThat(result.loginId()).isEqualTo("tester1");
            assertThat(result.externalId()).isNotNull();
            assertThat(result.name()).isEqualTo("홍길동");
            assertThat(result.contact()).isEqualTo("010-1111-2222");
            assertThat(result.status()).isEqualTo(MemberStatus.ACTIVE);
            assertThat(result.role()).isEqualTo(MemberRole.USER);

            em.flush();
            em.clear();

            // DB 실제 저장 여부 확인
            Member foundMember = memberRepository.findByLoginId("tester1").orElseThrow();
            assertThat(memberRepository.existsByLoginId("tester1")).isTrue();
            assertThat(foundMember.getRole()).isEqualTo(MemberRole.USER);
        }

        @Test
        @DisplayName("가맹점 가입 시 계좌 자동 생성")
        void merchantSignUp_WithAccount_Success() {
            // given
            var command = new MemberUsecase.MerchantSignUpCommand(
                    "merchant1", PASSWORD, "1234","카페주인", "010-3333-4444",
                    "123-45-67890", "맛있는카페", "음식점"
            );

            // when
            var result = memberService.merchantSignUp(command);

            // then
            assertThat(result.merchantName()).isEqualTo("맛있는카페");

            // 실제 계좌가 생성되었는지 확인 (Member ID로 조회)
            var member = memberRepository.findByExternalId(result.externalId()).orElseThrow();
            var accountExists = accountRepository.existsByMember(member);
            assertThat(accountExists).isTrue();
        }

        @Test
        @DisplayName("중복 아이디 가입 시 비즈니스 예외 발생 확인")
        void signUp_DuplicateId_ThrowsException() {
            // given
            var command1 = new MemberUsecase.MemberSignUpCommand("user1", PASSWORD, "홍길동", "010");
            memberService.signUp(command1);

            var command2 = new MemberUsecase.MemberSignUpCommand("user1", PASSWORD, "이순신", "010");

            // when & then
            assertThatThrownBy(() -> memberService.signUp(command2))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_LOGIN_ID);
        }
    }

    @Nested
    @DisplayName("정보 수정 및 탈퇴")
    class ManagementTest {

        @Test
        @DisplayName("정보 수정 성공")
        void updateMyInfo_Success() {
            // given
            var signup = memberService.signUp(new MemberUsecase.MemberSignUpCommand("user2", PASSWORD, "이름", "010"));
            var updateCommand = new MemberUsecase.MemberUpdateCommand(null, "새이름", "010-9999-9999");

            // when
            var result = memberService.updateMyInfo(signup.externalId(), updateCommand);

            // then
            assertThat(result.name()).isEqualTo("새이름");
            assertThat(result.contact()).isEqualTo("010-9999-9999");
        }

        @Test
        @DisplayName("회원 탈퇴 시 상태가 변경")
        void withdraw_StatusChanges() {
            // given
            var signup = memberService.signUp(new MemberUsecase.MemberSignUpCommand("user3", PASSWORD, "이름", "010"));

            // when
            memberService.withdraw(signup.externalId());

            // then
            var member = memberRepository.findByExternalId(signup.externalId()).orElseThrow();
            assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        }
    }

    @Nested
    @DisplayName("조회 검증")
    class QueryTest {
        @Test
        @DisplayName("회원 정보 조회 성공")
        void getMemberInfo_Success() {
            // given
            var signup = memberService.signUp(new MemberUsecase.MemberSignUpCommand("user4", PASSWORD, "이름", "010"));

            // when
            var result = memberService.getMemberInfo(signup.externalId());

            // then
            assertThat(result.loginId()).isEqualTo("user4");
            assertThat(result.name()).isEqualTo("이름");
        }

        @Test
        @DisplayName("가맹점 정보 조회 성공")
        void getMerchantInfo_Success() {
            // given
            var signup = memberService.merchantSignUp(new MemberUsecase.MerchantSignUpCommand(
                    "merchant2", PASSWORD, "5678","가맹점주", "010-5555-6666",
                    "987-65-43210", "멋진가게", "소매점"
            ));

            // when
            var result = memberService.getMyMerchantInfo(signup.externalId());

            // then
            assertThat(result.loginId()).isEqualTo("merchant2");
            assertThat(result.merchantName()).isEqualTo("멋진가게");
        }

        @Test
        @DisplayName("관리자 정보 조회 성공")
        void getAdminInfo_Success() {
            // given
            var admin = memberService.adminSignUp(new MemberUsecase.AdminSignUpCommand(
                    "admin1", PASSWORD, "관리자", "010-7777-8888", "SYSTEM"
            ));

            // when
            var result = memberService.getMyAdminInfo(admin.externalId());

            // then
            assertThat(result.loginId()).isEqualTo("admin1");
            assertThat(result.name()).isEqualTo("관리자");
            assertThat(result.department()).isEqualTo("SYSTEM");
        }
    }
}