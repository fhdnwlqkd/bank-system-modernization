package com.m2nsteel.bank_program_modernization.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.m2nsteel.bank_program_modernization.TestRedisConfig;
import com.m2nsteel.bank_program_modernization.core.api.ExceptionResponse;
import com.m2nsteel.bank_program_modernization.dto.AccountDto;
import com.m2nsteel.bank_program_modernization.dto.AuthDto;
import com.m2nsteel.bank_program_modernization.dto.MemberDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestRedisConfig.class)
@AutoConfigureJson
@Transactional
class AccountControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JsonMapper jsonmapper;

    private String accessToken;
    private String accountId;
    private final String LOGIN_ID = "banker123";
    private final String PASSWORD = "Password123!";
    private final String ACCOUNT_PW = "1234";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @AfterEach
    void tearDown() {
        Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection().serverCommands().flushAll();
    }

    @BeforeEach
    void setUp() throws JsonProcessingException {
        // 1. 회원가입
        var signUpReq = new MemberDto.MemberSignUpRequest(LOGIN_ID, PASSWORD, "홍길동", "010-1111-2222");
        mvc.post().uri("/api/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(signUpReq))
                .exchange();

        // 2. 로그인하여 토큰 획득
        var loginReq = new AuthDto.LoginRequest(LOGIN_ID, PASSWORD);
        assertThat(mvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(loginReq)))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.data.accessToken")
                .satisfies(token -> this.accessToken = token.toString());
    }

    @Test
    @DisplayName("성공: 신규 계좌 개설 및 전수 검증")
    void createAccount_Success() throws JsonProcessingException {
        var request = new AccountDto.AccountCreateRequest(ACCOUNT_PW);

        assertThat(mvc.post().uri("/api/accounts")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(request)))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.data")
                .convertTo(AccountDto.AccountResponse.class)
                .satisfies(res -> {
                    assertThat(res.externalId()).isNotBlank();
                    assertThat(res.accountNumber()).isNotBlank();
                    assertThat(res.balance()).isEqualTo(0L);
                    assertThat(res.status()).isEqualTo("ACTIVE");
                    assertThat(res.createdAt()).isNotNull();
                });
    }

    @Test
    @DisplayName("성공: 특정 계좌 상세 조회 (ID 추출 후 조회)")
    void getAccountDetail_Success() throws JsonProcessingException {
        // 1. 계좌 생성 후 ID만 쏙 뽑아오기
        var request = new AccountDto.AccountCreateRequest(ACCOUNT_PW);
        assertThat(mvc.post().uri("/api/accounts")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(request)))
                .bodyJson()
                .extractingPath("$.data.externalId")
                .satisfies(id -> accountId = id.toString());

        // 2. 추출한 ID로 상세 조회 검증
        assertThat(mvc.get().uri("/api/accounts/{id}", accountId)
                .header("Authorization", "Bearer " + accessToken))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.data")
                .convertTo(AccountDto.AccountResponse.class)
                .satisfies(res -> {
                    assertThat(res.externalId()).isEqualTo(accountId);
                    assertThat(res.balance()).isZero();
                });
    }

    @Test
    @DisplayName("성공: 내 계좌 목록 조회")
    void getMyAccounts_Success() throws JsonProcessingException {
        // Given: 계좌 2개 생성
        var request = new AccountDto.AccountCreateRequest(ACCOUNT_PW);
        mvc.post().uri("/api/accounts").header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON).content(jsonmapper.writeValueAsString(request)).exchange();
        mvc.post().uri("/api/accounts").header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON).content(jsonmapper.writeValueAsString(request)).exchange();

        // When & Then
        assertThat(mvc.get().uri("/api/members/me/accounts")
                .header("Authorization", "Bearer " + accessToken))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.data")
                .asArray().hasSize(2);
    }

    @Test
    @DisplayName("실패: 잘못된 비밀번호 형식 (숫자 4자리 미만)")
    void createAccount_Fail_InvalidPassword() throws JsonProcessingException {
        var request = new AccountDto.AccountCreateRequest("12"); // 4자리 미만
        assertThat(mvc.post().uri("/api/accounts")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(request)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.error")
                .convertTo(ExceptionResponse.class)
                .satisfies(res -> assertThat(res.code()).isEqualTo("C001")); // Common Invalid Input
    }

    @Test
    @DisplayName("실패: 존재하지 않는 계좌 상세 조회 시 404")
    void getAccountDetail_Fail_NotFound() {
        String randomId = UUID.randomUUID().toString();

        assertThat(mvc.get().uri("/api/accounts/{id}", randomId)
                .header("Authorization", "Bearer " + accessToken))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.error")
                .convertTo(ExceptionResponse.class)
                .satisfies(res -> assertThat(res.errorName()).contains("ACCOUNT_NOT_FOUND"));
    }

    @Test
    @DisplayName("성공: 계좌 비밀번호 변경")
    void changePassword_Success() throws JsonProcessingException {
        // 1. Given: 계좌 생성 및 ID 추출
        var createReq = new AccountDto.AccountCreateRequest(ACCOUNT_PW);
        assertThat(mvc.post().uri("/api/accounts")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(createReq)))
                .bodyJson()
                .extractingPath("$.data.externalId")
                .satisfies(id -> this.accountId = id.toString());

        // 2. When: 비밀번호 변경 실행 (1234 -> 5678)
        var changeReq = new AccountDto.AccountChangePasswordRequest(ACCOUNT_PW, "5678");
        assertThat(mvc.patch().uri("/api/accounts/{id}/password", accountId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(changeReq)))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.data")
                .convertTo(AccountDto.AccountResponse.class)
                .satisfies(res -> {
                    assertThat(res.externalId()).isEqualTo(accountId);
                    assertThat(res.status()).isEqualTo("ACTIVE");
                });
    }

    @Test
    @DisplayName("실패: 틀린 기존 비밀번호로 변경 시도 시 INVALID_PASSWORD 에러")
    void changePassword_Fail_WrongPassword() throws JsonProcessingException {
        // 1. Given: 계좌 생성
        var createReq = new AccountDto.AccountCreateRequest(ACCOUNT_PW);
        assertThat(mvc.post().uri("/api/accounts")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(createReq)))
                .bodyJson()
                .extractingPath("$.data.externalId")
                .satisfies(id -> this.accountId = id.toString());

        // 2. When: 엉뚱한 기존 비밀번호(0000)로 변경 시도
        var changeReq = new AccountDto.AccountChangePasswordRequest("0000", "5678");
        assertThat(mvc.patch().uri("/api/accounts/{id}/password", accountId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(changeReq)))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .extractingPath("$.error")
                .convertTo(ExceptionResponse.class)
                .satisfies(res -> {
                    assertThat(res.errorName()).contains("INVALID_PASSWORD");
                });
    }

    @Test
    @DisplayName("성공: 계좌 해지 및 상태 변경 검증")
    void closeAccount_Success() throws JsonProcessingException {
        // 1. Given: 계좌 생성
        var createReq = new AccountDto.AccountCreateRequest(ACCOUNT_PW);
        assertThat(mvc.post().uri("/api/accounts")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(createReq)))
                .bodyJson()
                .extractingPath("$.data.externalId")
                .satisfies(id -> this.accountId = id.toString());

        // 2. When: 계좌 해지 호출
        assertThat(mvc.delete().uri("/api/accounts/{id}", accountId)
                .header("Authorization", "Bearer " + accessToken))
                .hasStatus(HttpStatus.OK);
    }
}