package com.m2nsteel.bank_program_modernization.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.m2nsteel.bank_program_modernization.core.api.ExceptionResponse;
import com.m2nsteel.bank_program_modernization.dto.AuthDto;
import com.m2nsteel.bank_program_modernization.dto.CardDto;
import com.m2nsteel.bank_program_modernization.usecase.AccountUsecase;
import com.m2nsteel.bank_program_modernization.service.MemberService;
import com.m2nsteel.bank_program_modernization.service.AccountService;
import com.m2nsteel.bank_program_modernization.usecase.MemberUsecase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJson
@Transactional
class CardControllerTest {

    @Autowired private MockMvcTester mvc;
    @Autowired private JsonMapper jsonmapper;

    // Setup을 위한 서비스
    @Autowired private MemberService memberService;
    @Autowired private AccountService accountService;

    private String accessToken;
    private String memberExternalId;
    private String accountNumber;
    private String cardExternalId;

    private final String LOGIN_ID = "cardTester";
    private final String PW = "Password123!";
    private final String ACC_PW = "1234";

    @BeforeEach
    void setUp() throws JsonProcessingException {
        // 1. Service Layer를 통한 데이터 준비
        var memberResult = memberService.signUp(new MemberUsecase.MemberSignUpCommand(LOGIN_ID, PW, "길동", "010-1234-5678"));
        this.memberExternalId = memberResult.externalId();

        var accountResult = accountService.createAccount(new AccountUsecase.AccountCreateCommand(memberExternalId, ACC_PW));
        this.accountNumber = accountResult.accountNumber();

        // 2. MockMvc를 통한 로그인
        var loginReq = new AuthDto.LoginRequest(LOGIN_ID, PW);
        assertThat(mvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(loginReq)))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.data.accessToken")
                .satisfies(token -> this.accessToken = token.toString());
    }

    @Test
    @DisplayName("성공: 카드 발급 및 필드 검증")
    void issueCard_Success() throws JsonProcessingException {
        var request = new CardDto.CardIssueRequest(accountNumber, ACC_PW, "5678", "CHECK");

        assertThat(mvc.post().uri("/api/cards")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(request)))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.data")
                .convertTo(CardDto.CardResponse.class)
                .satisfies(res -> {
                    assertThat(res.cardNumber()).isNotBlank();
                    assertThat(res.accountNumber()).isEqualTo(accountNumber);
                    assertThat(res.cardType()).isEqualTo("CHECK");
                    assertThat(res.status()).isEqualTo("ACTIVE");
                    assertThat(res.expiredAt()).isNotNull();
                });
    }

    @Test
    @DisplayName("성공: 특정 카드 상세 조회")
    void getCardDetail_Success() throws JsonProcessingException {
        // 1. 카드 먼저 발급
        String cardId = issueCardAndGetId();

        // 2. 조회 및 검증
        assertThat(mvc.get().uri("/api/cards/{cardId}", cardId)
                .header("Authorization", "Bearer " + accessToken))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.data.externalId")
                .isEqualTo(cardId);
    }

    @Test
    @DisplayName("성공: 내 카드 목록 조회")
    void getMyCards_Success() throws JsonProcessingException {
        issueCardAndGetId();
        issueCardAndGetId();

        assertThat(mvc.get().uri("/api/members/me/cards")
                .header("Authorization", "Bearer " + accessToken))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.data")
                .asArray().hasSize(2);
    }

    @Test
    @DisplayName("실패: 계좌 비밀번호 불일치 시 카드 발급 실패 (401)")
    void issueCard_Fail_WrongAccountPassword() throws JsonProcessingException {
        var request = new CardDto.CardIssueRequest(accountNumber, "9999", "5678", "CHECK");

        assertThat(mvc.post().uri("/api/cards")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(request)))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson()
                .extractingPath("$.error")
                .convertTo(ExceptionResponse.class)
                .satisfies(res -> assertThat(res.errorName()).contains("INVALID_PASSWORD"));
    }

    @Test
    @DisplayName("실패: 존재하지 않는 카드를 조회할 때 (404)")
    void getCardDetail_Fail_NotFound() {
        String randomId = UUID.randomUUID().toString();

        assertThat(mvc.get().uri("/api/cards/{cardId}", randomId)
                .header("Authorization", "Bearer " + accessToken))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    // --- Helper ---
    private String issueCardAndGetId() throws JsonProcessingException {
        var request = new CardDto.CardIssueRequest(accountNumber, ACC_PW, "5678", "CHECK");
        assertThat(mvc.post().uri("/api/cards")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(request)))
                .bodyJson()
                .extractingPath("$.data.externalId")
                .satisfies(id -> this.cardExternalId = id.toString());
        return this.cardExternalId;
    }
}