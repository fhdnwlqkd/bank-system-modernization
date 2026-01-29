package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.domain.constant.MemberStatus;
import com.m2nsteel.bank_program_modernization.repository.AccountRepository;
import com.m2nsteel.bank_program_modernization.repository.MemberRepository;
import com.m2nsteel.bank_program_modernization.usecase.MemberUsecase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

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

    private static final String PASSWORD = "password123!";

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

            // then
            assertThat(result.loginId()).isEqualTo("tester1");
            assertThat(result.externalId()).isNotNull();

            // DB 실제 저장 여부 확인
            assertThat(memberRepository.existsByLoginId("tester1")).isTrue();
        }

        @Test
        @DisplayName("가맹점 가입 시 계좌 자동 생성")
        void merchantSignUp_WithAccount_Success() {
            // given
            var command = new MemberUsecase.MerchantSignUpCommand(
                    "merchant1", PASSWORD, "카페주인", "010-3333-4444",
                    "123-45-67890", "맛있는카페", "음식점"
            );

            // when
            var result = memberService.merchantSignUp(command);

            // then
            assertThat(result.shopName()).isEqualTo("맛있는카페");

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
}