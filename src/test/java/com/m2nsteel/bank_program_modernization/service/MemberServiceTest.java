package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.domain.Branch;
import com.m2nsteel.bank_program_modernization.domain.Member;
import com.m2nsteel.bank_program_modernization.dto.request.BranchCreateRequest;
import com.m2nsteel.bank_program_modernization.dto.request.MemberSignUpRequest;
import com.m2nsteel.bank_program_modernization.dto.response.BranchResponse;
import com.m2nsteel.bank_program_modernization.repository.MemberRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberServiceTest {
    @Autowired MemberService memberService;
    @Autowired MemberRepository memberRepository;
    @Autowired BranchService branchService;
    private String branchCode;

    @BeforeEach
    void setUp() {
        // 1. 지점 생성
        BranchResponse branch = branchService.createBranch(
                new BranchCreateRequest(
                        "Test Branch",
                        "123 Test St",
                        "555-0000"
                )
        );
        this.branchCode = branch.branchCode();
    }

    @Test
    @DisplayName("회원가입 성공 테스트")
    void signup_success() {
        MemberSignUpRequest request = new MemberSignUpRequest(
                "testuser",
                "password123",
                "John Doe",
                branchCode
        );
        var response = memberService.signUp(request);
        assertThat(response.loginId()).isEqualTo("testuser");
        assertThat(memberRepository.existsByLoginId("testuser")).isTrue();
    }

    @Test
    @DisplayName("중복 아이디 가입 시 BusinessException 발생")
    void signup_duplicate_fail() {
        MemberSignUpRequest request1 = new MemberSignUpRequest(
                "testuser",
                "password123",
                "John Doe",
                branchCode
        );
        var response = memberService.signUp(request1);

        MemberSignUpRequest request2 = new MemberSignUpRequest(
                "testuser",
                "pw123",
                "중복이",
                branchCode);
        assertThatThrownBy(() -> memberService.signUp(request2))
                .isInstanceOf(BusinessException.class);
    }
}