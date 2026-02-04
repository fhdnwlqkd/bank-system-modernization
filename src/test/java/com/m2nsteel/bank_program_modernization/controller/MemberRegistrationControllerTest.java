package com.m2nsteel.bank_program_modernization.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.m2nsteel.bank_program_modernization.TestRedisConfig;
import com.m2nsteel.bank_program_modernization.core.api.ExceptionResponse;
import com.m2nsteel.bank_program_modernization.domain.constant.MemberRole;
import com.m2nsteel.bank_program_modernization.domain.constant.MemberStatus;
import com.m2nsteel.bank_program_modernization.dto.AuthDto;
import com.m2nsteel.bank_program_modernization.dto.MemberDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJson
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
class MemberRegistrationControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JsonMapper jsonmapper;

    // --- 1. 일반 회원 (Member) 테스트 ---
    @Autowired
    private StringRedisTemplate redisTemplate;

    @AfterEach
    void tearDown() {
        Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection().serverCommands().flushAll();
    }
    @Test
    @DisplayName("성공: 일반 회원 가입 시 ApiResponse 내 data 필드 검증")
    void signUp_Success() throws JsonProcessingException {
        var request = new MemberDto.MemberSignUpRequest("user123", "Pass1234!", "홍길동", "010-1234-5678");

        assertThat(mvc.post().uri("/api/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(request)))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.data")
                .convertTo(MemberDto.MemberResponse.class)
                .satisfies(res -> {
                    assertThat(res.loginId()).isEqualTo("user123");
                    assertThat(res.name()).isEqualTo("홍길동");
                    assertThat(res.status()).isEqualTo(MemberStatus.ACTIVE);
                    assertThat(res.role()).isEqualTo(MemberRole.USER);
                });
    }

    // --- 2. 가맹점 (Merchant) 테스트 ---

    @Test
    @DisplayName("성공: 가맹점 가입 시 data 필드 객체 변환")
    void merchantSignUp_Success() throws JsonProcessingException {
        var request = new MemberDto.MerchantSignUpRequest(
                "merchant_boss", "Pass1234!", "1234", "길동", "010-1111-2222",
                "123-45-67890", "길동네카페", "FOOD");

        assertThat(mvc.post().uri("/api/merchants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(request)))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.data")
                .convertTo(MemberDto.MerchantSignUpResponse.class)
                .satisfies(res -> {
                    assertThat(res.loginId()).isEqualTo("merchant_boss");
                    assertThat(res.merchantName()).isEqualTo("길동네카페");
                });
    }

    // --- 4. 로그인 (Auth) 테스트 ---

    @Test
    @DisplayName("성공: 로그인 시 토큰이 data 필드에 담겨 오는지 확인")
    void login_Success() throws JsonProcessingException {
        signUp_Success();

        var loginRequest = new AuthDto.LoginRequest("user123", "Pass1234!");

        assertThat(mvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(loginRequest)))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.data")
                .convertTo(AuthDto.TokenResponse.class)
                .satisfies(res -> {
                    assertThat(res.accessToken()).isNotBlank();
                    assertThat(res.refreshToken()).isNotBlank();
                });
    }

    // --- 5. 예외 처리 테스트 ---
    @Test
    @DisplayName("실패: 비밀번호 불일치 시 ErrorResponse 객체 검증")
    void login_Fail_InvalidPassword() throws JsonProcessingException {
        signUp_Success();

        var loginRequest = new AuthDto.LoginRequest("user123", "WrongPass!");

        assertThat(mvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(loginRequest)))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .extractingPath("$.error")
                .convertTo(ExceptionResponse.class)
                .satisfies(res -> {
                    assertThat(res.code()).isEqualTo("M003");
                    assertThat(res.errorName()).isEqualTo("INVALID_PASSWORD");
                });
    }
}