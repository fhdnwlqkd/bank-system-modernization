package com.m2nsteel.bank_program_modernization.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.m2nsteel.bank_program_modernization.core.exception.ErrorResponse;
import com.m2nsteel.bank_program_modernization.domain.constant.MemberStatus;
import com.m2nsteel.bank_program_modernization.dto.AuthDto;
import com.m2nsteel.bank_program_modernization.dto.MemberDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJson
@Transactional
class MemberControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JsonMapper jsonmapper;

    // --- 1. 일반 회원 (Member) 테스트 ---

    @Test
    @DisplayName("성공: 일반 회원 가입 및 객체 변환 검증")
    void signUp_Success() throws JsonProcessingException {
        var request = new MemberDto.MemberSignUpRequest("user123", "Pass1234!", "홍길동", "010-1234-5678");

        assertThat(mvc.post().uri("/api/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(request)))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .convertTo(MemberDto.MemberResponse.class)
                .satisfies(res -> {
                    assertThat(res.loginId()).isEqualTo("user123");
                    assertThat(res.name()).isEqualTo("홍길동");
                    assertThat(res.contact()).isEqualTo("010-1234-5678");
                    assertThat(res.status()).isEqualTo(MemberStatus.ACTIVE);
                    assertThat(res.externalId()).isNotNull();
                });
    }

    // --- 2. 가맹점 (Merchant) 테스트 ---

    @Test
    @DisplayName("성공: 가맹점 가입 및 상세 객체 검증")
    void merchantSignUp_Success() throws JsonProcessingException {
        var request = new MemberDto.MerchantSignUpRequest(
                "merchant_boss", "Pass1234!", "1234", "길동", "010-1111-2222",
                "123-45-67890", "길동네카페", "FOOD");

        assertThat(mvc.post().uri("/api/merchants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(request)))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .convertTo(MemberDto.MerchantSignUpResponse.class)
                .satisfies(res -> {
                    assertThat(res.loginId()).isEqualTo("merchant_boss");
                    assertThat(res.merchantName()).isEqualTo("길동네카페");
                    assertThat(res.businessNumber()).isEqualTo("123-45-67890");
                    assertThat(res.accountNumber()).isNotBlank();
                    assertThat(res.category()).isEqualTo("FOOD");
                });
    }

    // --- 3. 관리자 (Admin) 테스트 ---

    @Test
    @DisplayName("성공: 관리자 가입 및 부서 정보 검증")
    void adminSignUp_Success() throws JsonProcessingException {
        var request = new MemberDto.AdminSignUpRequest("admin_root", "Admin123!", "관리자A", "010-9999-8888", "IT_SECURITY");

        assertThat(mvc.post().uri("/api/admins")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(request)))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .convertTo(MemberDto.AdminResponse.class)
                .satisfies(res -> {
                    assertThat(res.loginId()).isEqualTo("admin_root");
                    assertThat(res.department()).isEqualTo("IT_SECURITY");
                });
    }

    // --- 4. 로그인 (Auth) 테스트 ---

    @Test
    @DisplayName("성공: 로그인 후 토큰 객체 검증")
    void login_Success() throws JsonProcessingException {
        signUp_Success(); // 사전 데이터 준비

        var loginRequest = new AuthDto.LoginRequest("user123", "Pass1234!");

        assertThat(mvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(loginRequest)))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .convertTo(AuthDto.TokenResponse.class)
                .satisfies(res -> {
                    assertThat(res.accessToken()).isNotBlank();
                    assertThat(res.refreshToken()).isNotBlank();
//                    assertThat(res.grantType()).isEqualTo("Bearer");
                });
    }

    @Test
    @DisplayName("실패: 비밀번호 불일치 시 401 Unauthorized 및 ErrorResponse 검증")
    void login_Fail_InvalidPassword() throws JsonProcessingException {
        // Given: 먼저 정상 가입
        signUp_Success();

        // When: 틀린 비밀번호로 로그인 요청
        var loginRequest = new AuthDto.LoginRequest("user123", "WrongPass!");

        assertThat(mvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(loginRequest)))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .convertTo(ErrorResponse.class)
                .satisfies(res -> {
                    assertThat(res.code()).isEqualTo("M003");
                    assertThat(res.errorName()).isEqualTo("INVALID_PASSWORD");
                    assertThat(res.message()).contains("비밀번호");
                    assertThat(res.timestamp()).isNotNull();
                    assertThat(res.errors()).isEmpty();
                });
    }

    // --- 5. 예외 처리 (Exception Handling) 테스트 ---
    @Test
    @DisplayName("실패: 중복 가입 시 BusinessException -> ErrorResponse 객체 검증")
    void signUp_Fail_DuplicateId() throws JsonProcessingException {
        // Given: 사전 가입
        signUp_Success();

        // When: 중복 ID로 가입 시도
        var request = new MemberDto.MemberSignUpRequest("user123", "Pass1234!", "홍길동", "010-1234-5678");

        assertThat(mvc.post().uri("/api/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(request)))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .convertTo(ErrorResponse.class)
                .satisfies(res -> {
                    assertThat(res.code()).isEqualTo("M001");
                    assertThat(res.errorName()).isEqualTo("DUPLICATE_LOGIN_ID");
                    assertThat(res.message()).contains("이미 존재");
                    assertThat(res.timestamp()).isBeforeOrEqualTo(LocalDateTime.now());
                    assertThat(res.errors()).isEmpty();
                });
    }
}