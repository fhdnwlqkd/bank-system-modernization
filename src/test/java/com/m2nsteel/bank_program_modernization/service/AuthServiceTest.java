package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.repository.MemberRepository;
import com.m2nsteel.bank_program_modernization.usecase.AuthUsecase;
import com.m2nsteel.bank_program_modernization.usecase.MemberUsecase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Transactional
class AuthServiceTest {
    @Autowired AuthService authService;
    @Autowired MemberService memberService;
    @Autowired MemberRepository memberRepository;

    private final String LOGIN_ID = "tester123";
    private final String PASSWORD = "password123!";

    @BeforeEach
    void setUp() {
        var command = new MemberUsecase.MemberSignUpCommand(
                LOGIN_ID, PASSWORD, "홍길동", "010-1111-2222"
        );

        var result = memberService.signUp(command);
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        // Given
        AuthUsecase.LoginCommand command = new AuthUsecase.LoginCommand(LOGIN_ID, PASSWORD);

        // When
        AuthUsecase.TokenResult response = authService.login(command);

        // Then
        assertThat(response.accessToken()).isNotEmpty();
        assertThat(response.refreshToken()).isNotEmpty();
    }

    @Test
    @DisplayName("로그인 실패: 비밀번호 불일치")
    void login_fail_invalid_password() {
        // Given
        AuthUsecase.LoginCommand command = new AuthUsecase.LoginCommand(LOGIN_ID, "wrong_password");

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                authService.login(command));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_PASSWORD);
    }

    @Test
    @DisplayName("로그인 실패: 존재하지 않는 아이디")
    void login_fail_member_not_found() {
        // Given
        AuthUsecase.LoginCommand command = new AuthUsecase.LoginCommand("non_existent_id", PASSWORD);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                authService.login(command));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("본인 확인 성공")
    void verifyMember_success() {
        // Given
        Long memberId = memberRepository.findByLoginId(LOGIN_ID).get().getId();

        // When & Then (예외가 발생하지 않아야 함)
        authService.verifyMember(memberId, PASSWORD);
    }
}