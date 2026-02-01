package com.m2nsteel.bank_program_modernization.controller;

import com.jayway.jsonpath.JsonPath;
import com.m2nsteel.bank_program_modernization.dto.AuthDto;
import com.m2nsteel.bank_program_modernization.dto.MerchantDto;
import com.m2nsteel.bank_program_modernization.service.*;
import com.m2nsteel.bank_program_modernization.usecase.*;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJson
@Transactional
class MerchantControllerTest {

    @Autowired private MockMvcTester mvc;
    @Autowired private JsonMapper jsonmapper;

    @Autowired private MemberService memberService;
    @Autowired private AccountService accountService;
    @Autowired private CardService cardService;
    @Autowired private TransactionService transactionService;

    private String merchantToken;
    private final String MERCHANT_ID = "merchant123";
    private final String PASSWORD = "password123!";
    private final String BRN = "123-45-67890";

    @BeforeEach
    void setUp() throws Exception {
        // 1. 가맹점주 및 상점 등록 (서비스 레이어 활용)
        var merchantCommand = new MemberUsecase.MerchantSignUpCommand(
                MERCHANT_ID, PASSWORD, "1234", "길동사장님", "010-1234-5678",
                BRN, "길동치킨", "음식점"
        );
        memberService.merchantSignUp(merchantCommand);

        // 2. 결제를 발생시킬 일반 사용자 및 카드 준비
        var user = memberService.signUp(new MemberUsecase.MemberSignUpCommand("user1", PASSWORD, "구매자", "010-9999-8888"));
        var userAcc = accountService.createAccount(new AccountUsecase.AccountCreateCommand(user.externalId(), PASSWORD));
        transactionService.deposit(new TransactionUsecase.DepositCommand(userAcc.accountNumber(), 100000L, "idemp-setup"), user.externalId());
        var card = cardService.issueCard(new CardUsecase.IssueCardCommand(userAcc.accountNumber(), PASSWORD, "1111", "CHECK"), user.externalId());

        // 3. 테스트용 결제 데이터 5건 생성 (10,000원씩 5건 = 50,000원)
        for (int i = 1; i <= 5; i++) {
            cardService.pay(new CardUsecase.CardPaymentCommand(
                    card.externalId(), 10000L, "1111", BRN, "pay-key-" + i
            ), user.externalId());
        }

        // 4. 가맹점주 토큰 획득
        this.merchantToken = obtainAccessToken(MERCHANT_ID, PASSWORD);
    }

    @Test
    @DisplayName("성공: 가맹점 기간별 정산 요약 조회 (금액 및 건수 검증)")
    void getSettlementSummary_Success() {
        LocalDate today = LocalDate.now();

        assertThat(mvc.get().uri("/api/merchants/me/settlements")
                .header("Authorization", "Bearer " + merchantToken)
                .param("from", today.toString())
                .param("to", today.toString()))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.data")
                .convertTo(MerchantDto.SalesSummaryResponse.class)
                .satisfies(res -> {
                    assertThat(res.totalSalesAmount()).isEqualTo(50000L);
                    assertThat(res.totalTransactionCount()).isEqualTo(5L);
                    assertThat(res.netAmount()).isEqualTo(50000L);
                });
    }

    @Test
    @DisplayName("성공: 가맹점 결제 내역 조회 및 페이징 확인")
    void getPayments_Pagination_Success() {
        assertThat(mvc.get().uri("/api/merchants/me/payments")
                .header("Authorization", "Bearer " + merchantToken)
                .param("size", "3")) // 3개만 조회
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.data")
                .convertTo(CursorResult.class)
                .satisfies(data -> {
                    assertThat(data.values()).hasSize(3);
                    assertThat(data.hasNext()).isEqualTo(true);
                    assertThat(data.nextCursor()).isNotNull();
                });
    }

    @Test
    @DisplayName("실패: 일반 사용자가 가맹점 전용 API 접근 시 거부")
    void getSettlementSummary_Forbidden_RegularUser() throws Exception {
        // 1. Given: 가맹점이 아닌 일반 유저 토큰 생성
        memberService.signUp(new MemberUsecase.MemberSignUpCommand("normalUser", PASSWORD, "일반인", "010-0000-0000"));
        String userToken = obtainAccessToken("normalUser", PASSWORD);

        // 2. When & Then: 접근 시도 시 403 혹은 서비스 레이어의 권한 에러 확인
        assertThat(mvc.get().uri("/api/merchants/me/settlements")
                .header("Authorization", "Bearer " + userToken)
                .param("from", LocalDate.now().toString())
                .param("to", LocalDate.now().toString()))
                .hasStatus(HttpStatus.FORBIDDEN); // Service에서 가맹점주 조회 실패 시
    }


    /**
     * 로그인을 통해 토큰을 발급받는 헬퍼 메서드
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