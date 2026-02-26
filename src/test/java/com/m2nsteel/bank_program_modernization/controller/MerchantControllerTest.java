package com.m2nsteel.bank_program_modernization.controller;

import com.jayway.jsonpath.JsonPath;
import com.m2nsteel.bank_program_modernization.dto.AuthDto;
import com.m2nsteel.bank_program_modernization.dto.MerchantDto;
import com.m2nsteel.bank_program_modernization.service.*;
import com.m2nsteel.bank_program_modernization.usecase.*;
import org.junit.jupiter.api.AfterEach;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Objects;

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
        // 1. 가맹점주 및 상점 등록
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

        // 3. 테스트용 결제 데이터 5건 생성 (10,000원씩 5건 = 총 50,000원)
        for (int i = 1; i <= 5; i++) {
            cardService.pay(new CardUsecase.CardPaymentCommand(
                    card.externalId(), 10000L, "1111", BRN, "pay-key-" + i
            ), user.externalId());
        }

        // 4. 가맹점주 토큰 획득
        this.merchantToken = obtainAccessToken(MERCHANT_ID, PASSWORD);
    }

    @Test
    @DisplayName("성공: 가맹점 기간별 정산 요약 조회 (환불 없는 초기 상태)")
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
    @DisplayName("성공: 환불 발생 시 정산 요약 수치 검증 (집계 정합성 확인)")
    void getSettlementSummary_WithRefund_Success() throws Exception {
        // 1. Given: 기존 5건(50,000원) 중 최신 결제 1건을 찾아 환불 처리 
        var exchange = mvc.get().uri("/api/merchants/me/payments")
                .header("Authorization", "Bearer " + merchantToken)
                .param("size", "1")
                .exchange();

        // 응답 JSON 문자열 안전 추출
        String body = new String(exchange.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);

        // $.data.values[0] 노드를 DTO로 변환
        var root = jsonmapper.readTree(body);
        var firstValueNode = root.at("/data/values/0");

        var lastPayment = jsonmapper.treeToValue(firstValueNode, MerchantDto.PaymentHistoryResponse.class);

        // 서비스 레이어를 직접 호출하여 10,000원 전체 환불 수행
        cardService.refund(new CardUsecase.RefundCommand(
                lastPayment.externalPaymentId(),
                10000L,
                "테스트 환불",
                "refund-ref-1"
        ));

        // 2. When: 정산 요약 API 다시 호출 
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
                    // 3. Then: 환불 데이터가 반영된 집계 결과 검증 
                    // 총 매출 원금(정산 대상 총액)은 그대로 50,000원 
                    assertThat(res.totalSalesAmount()).isEqualTo(50000L);
                    // 완전히 환불된 1건을 제외한 유효 거래 건수는 4건 
                    assertThat(res.totalTransactionCount()).isEqualTo(4L);
                    // 환불액 집계 확인 (10,000원) 
                    assertThat(res.totalRefundAmount()).isEqualTo(10000L);
                    // 순 매출액 (50,000 - 10,000 = 40,000원) 
                    assertThat(res.netAmount()).isEqualTo(40000L);
                });
    }

    @Test
    @DisplayName("성공: 가맹점 결제 내역 조회 및 페이징 확인")
    void getPayments_Pagination_Success() {
        assertThat(mvc.get().uri("/api/merchants/me/payments")
                .header("Authorization", "Bearer " + merchantToken)
                .param("size", "3"))
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
    @DisplayName("실패: 일반 사용자가 가맹점 전용 API 접근 시 거부 (403)")
    void getSettlementSummary_Forbidden_RegularUser() throws Exception {
        memberService.signUp(new MemberUsecase.MemberSignUpCommand("normalUser", PASSWORD, "일반인", "010-0000-0000"));
        String userToken = obtainAccessToken("normalUser", PASSWORD);

        assertThat(mvc.get().uri("/api/merchants/me/settlements")
                .header("Authorization", "Bearer " + userToken)
                .param("from", LocalDate.now().toString())
                .param("to", LocalDate.now().toString()))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    private String obtainAccessToken(String loginId, String password) throws Exception {
        var loginRequest = new AuthDto.LoginRequest(loginId, password);

        var result = mvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonmapper.writeValueAsString(loginRequest))
                .exchange();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }
}