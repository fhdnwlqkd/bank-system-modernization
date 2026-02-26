package com.m2nsteel.bank_program_modernization.controller;

import com.jayway.jsonpath.JsonPath;
import com.m2nsteel.bank_program_modernization.dto.AuthDto;
import com.m2nsteel.bank_program_modernization.service.AccountService;
import com.m2nsteel.bank_program_modernization.service.MemberService;
import com.m2nsteel.bank_program_modernization.service.TransactionService;
import com.m2nsteel.bank_program_modernization.usecase.AccountUsecase;
import com.m2nsteel.bank_program_modernization.usecase.CursorResult;
import com.m2nsteel.bank_program_modernization.usecase.MemberUsecase;
import com.m2nsteel.bank_program_modernization.usecase.TransactionUsecase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJson
@Transactional
class TransactionControllerSearchTest {

    @Autowired private MockMvcTester mvc;
    @Autowired private JsonMapper jsonmapper;

    @Autowired private MemberService memberService;
    @Autowired private AccountService accountService;
    @Autowired private TransactionService transactionService;

    private String userToken;
    private String adminToken;
    private String userAccountExternalId;
    private String userAccountNumber;
    @BeforeEach
    void setUp() throws Exception {
        // 1. 일반 사용자 데이터 셋업 (서비스 레이어 활용)
        var user = memberService.signUp(new MemberUsecase.MemberSignUpCommand("user123", "Pass123!", "사용자", "010-1111-1111"));
        var account = accountService.createAccount(new AccountUsecase.AccountCreateCommand(user.externalId(), "1234"));
        this.userAccountExternalId = account.externalId();
        this.userAccountNumber = account.accountNumber();

        // 페이징 테스트를 위해 15개의 거래 내역 생성
        for (int i = 1; i <= 15; i++) {
            transactionService.deposit(
                    new TransactionUsecase.DepositCommand(userAccountNumber, 1000L * i, "idempotency-" + i),
                    user.externalId()
            );
        }

        // 2. 관리자 데이터 셋업
        memberService.adminSignUp(new MemberUsecase.AdminSignUpCommand("admin123", "Pass123!", "관리자", "010-9999-9999", "helloAdmin"));

        // 3. MockMvc를 이용한 실제 로그인 및 토큰 발급
        this.userToken = obtainAccessToken("user123", "Pass123!");
        this.adminToken = obtainAccessToken("admin123", "Pass123!");
    }

    @Test
    @DisplayName("성공: 첫 번째 페이지 조회 - 10개 데이터 및 다음 커서 존재 확인")
    void searchTransactions_FirstPage_Success() {
        assertThat(mvc.get().uri("/api/transactions/history")
                .header("Authorization", "Bearer " + userToken)
                .param("accountExternalId", userAccountExternalId)
                .param("size", "10"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.data")
                .convertTo(CursorResult.class)
                .satisfies(data -> {
                    assertThat(data.values()).hasSize(10);
                    assertThat(data.hasNext()).isEqualTo(true);
                    assertThat(data.nextCursor()).isNotNull();
                });
    }

    @Test
    @DisplayName("성공: 두 번째 페이지 조회 - 첫 페이지의 커서를 사용하여 남은 5개 데이터 확인")
    void searchTransactions_SecondPage_Success() {
        // 첫 번째 페이지 호출하여 커서 획득
        var firstPageResponse = mvc.get().uri("/api/transactions/history")
                .header("Authorization", "Bearer " + userToken)
                .param("accountExternalId", userAccountExternalId)
                .param("size", "10")
                .exchange();

        String body = new String(firstPageResponse.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        String nextCursor = jsonmapper.readTree(body).at("/data/nextCursor").asString();

        // 획득한 커서(lastId)를 파라미터로 넘겨 다음 페이지 호출
        assertThat(mvc.get().uri("/api/transactions/history")
                .header("Authorization", "Bearer " + userToken)
                .param("accountExternalId", userAccountExternalId)
                .param("lastId", nextCursor)
                .param("size", "10"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.data")
                .convertTo(CursorResult.class)
                .satisfies(data -> {
                    assertThat(data.values()).hasSize(5);
                    assertThat(data.hasNext()).isEqualTo(false);
                });
    }

    @Test
    @DisplayName("성공: 관리자 권한으로 본인 소유가 아닌 계좌의 거래 내역 조회")
    void searchTransactions_AdminAccess_Success() {
        assertThat(mvc.get().uri("/api/transactions/history")
                .header("Authorization", "Bearer " + adminToken)
                .param("accountExternalId", userAccountExternalId))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.data")
                .convertTo(CursorResult.class)
                .satisfies(data -> {
                    assertThat(data.values()).hasSize(10);
                    assertThat(data.hasNext()).isEqualTo(true);
                });
    }

    @Test
    @DisplayName("실패: 일반 사용자가 타인의 계좌 내역 조회 시 소유권 권한 에러 발생")
    void searchTransactions_NotOwner_Fail() throws Exception {
        // 다른 사용자(유효한 토큰 소유자) 생성
        memberService.signUp(new MemberUsecase.MemberSignUpCommand("otherUser", "Pass123!", "타인", "010-2222-2222"));
        String otherUserToken = obtainAccessToken("otherUser", "Pass123!");

        assertThat(mvc.get().uri("/api/transactions/history")
                .header("Authorization", "Bearer " + otherUserToken)
                .param("accountExternalId", userAccountExternalId))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson()
                .extractingPath("$.error.errorName")
                .asString().contains("NOT_ACCOUNT_OWNER");
    }

    /**
     * MockMvc를 통해 로그인 후 Access Token을 반환하는 헬퍼 메서드
     */
    private String obtainAccessToken(String loginId, String password) throws Exception {
        var loginRequest = new AuthDto.LoginRequest(loginId, password);

        var result = mvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(loginRequest))
                .exchange();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }
}