package com.m2nsteel.bank_program_modernization.service;

import com.m2nsteel.bank_program_modernization.core.exception.BusinessException;
import com.m2nsteel.bank_program_modernization.domain.Member;
import com.m2nsteel.bank_program_modernization.domain.MerchantMember;
import com.m2nsteel.bank_program_modernization.dto.request.BranchCreateRequest;
import com.m2nsteel.bank_program_modernization.dto.request.MemberSignUpRequest;
import com.m2nsteel.bank_program_modernization.dto.request.MerchantSignUpRequest;
import com.m2nsteel.bank_program_modernization.dto.response.BranchResponse;
import com.m2nsteel.bank_program_modernization.repository.MemberRepository;
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
    @DisplayName("가맹점 회원가입 성공 및 전용 필드 검증 테스트")
    void merchant_signup_success() {
        // 1. Given
        MerchantSignUpRequest request = new MerchantSignUpRequest(
                "merchantuser",
                "merchantpass",
                "맛있는 식당",
                branchCode,
                "123-45-67890", // 사업자 번호
                "RESTAURANT"    // 카테고리
        );

        // 2. When
        var response = memberService.merchantSignUp(request);

        // 3. Then
        assertThat(response.loginId()).isEqualTo("merchantuser");
        assertThat(response.businessRegistrationNumber()).isEqualTo("123-45-67890");

        // 4. DB 상세 검증
        Member foundMember = memberRepository.findByLoginId("merchantuser")
                .orElseThrow();

        assertThat(foundMember).isInstanceOf(MerchantMember.class); // 실제 클래스 타입 확인

        MerchantMember merchant = (MerchantMember) foundMember;
        assertThat(merchant.getBusinessRegistrationNumber()).isEqualTo("123-45-67890");
        assertThat(merchant.getMerchantCategory()).isEqualTo("RESTAURANT");
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