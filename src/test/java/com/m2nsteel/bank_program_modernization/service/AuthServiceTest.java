package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorCode;
import com.m2nsteel.bank_program_modernization.dto.request.BranchCreateRequest;
import com.m2nsteel.bank_program_modernization.dto.request.LoginRequest;
import com.m2nsteel.bank_program_modernization.dto.request.MemberSignUpRequest;
import com.m2nsteel.bank_program_modernization.dto.response.TokenResponse;
import com.m2nsteel.bank_program_modernization.repository.MemberRepository;
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
    @Autowired BranchService branchService;
    @Autowired MemberRepository memberRepository;

    private final String LOGIN_ID = "tester123";
    private final String RAW_PASSWORD = "password123!";

    @BeforeEach
    void setUp() {
        // 1. 지점 생성
        var branch = branchService.createBranch(
                new BranchCreateRequest("Branch1", "Address1", "Contact1")
        );
        // 2.가입 및 계좌 생성
        var member = memberService.signUp(new MemberSignUpRequest(LOGIN_ID, RAW_PASSWORD, "테스터", branch.branchCode()));
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        // Given
        LoginRequest request = new LoginRequest(LOGIN_ID, RAW_PASSWORD);

        // When
        TokenResponse response = authService.login(request);

        // Then
        assertThat(response.accessToken()).isNotEmpty();
        assertThat(response.refreshToken()).isNotEmpty();
    }

    @Test
    @DisplayName("로그인 실패: 비밀번호 불일치")
    void login_fail_invalid_password() {
        // Given
        LoginRequest request = new LoginRequest(LOGIN_ID, "wrong_password");

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                authService.login(request));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_PASSWORD);
    }

    @Test
    @DisplayName("로그인 실패: 존재하지 않는 아이디")
    void login_fail_member_not_found() {
        // Given
        LoginRequest request = new LoginRequest("non_existent_id", RAW_PASSWORD);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () ->
                authService.login(request));
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("본인 확인 성공")
    void verifyMember_success() {
        // Given
        Long memberId = memberRepository.findByLoginId(LOGIN_ID).get().getId();

        // When & Then (예외가 발생하지 않아야 함)
        authService.verifyMember(memberId, RAW_PASSWORD);
    }
}